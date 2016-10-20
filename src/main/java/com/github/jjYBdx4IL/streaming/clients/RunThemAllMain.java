package com.github.jjYBdx4IL.streaming.clients;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Timer;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jjYBdx4IL
 */
public class RunThemAllMain implements ChatListener, FollowerListener {

    private static final Logger LOG = LoggerFactory.getLogger(RunThemAllMain.class);
    private static final Timer CHATLOG_REMOVAL_TIMER = new Timer(true);

    public static void main(String[] args) {
        new RunThemAllMain().run();
    }

    public static void playSound(String fileName) {
        if (fileName == null) {
            return;
        }
        File soundFile = new File(fileName);
        if (!soundFile.exists()) {
            return;
        }
        play(soundFile);
    }

    public static void play(File file) {
        try {
            final Clip clip = (Clip) AudioSystem.getLine(new Line.Info(Clip.class));

            clip.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                }
            });

            clip.open(AudioSystem.getAudioInputStream(file));
            clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException exc) {
            LOG.error("", exc);
        }
    }

    private GenericConfig config = null;

    public void run() {
        try {
            config = (GenericConfig) GenericConfig.readConfig("generic.xml", GenericConfig.class);
            config.postprocess();
            
            TwitchClientConnectionManager twitchCCM = new TwitchClientConnectionManager();
            twitchCCM.addChatListener(this);
            twitchCCM.addFollowerListener(this);
            twitchCCM.start();
            
            HitBoxClientConnectionManager hitBoxCCM = new HitBoxClientConnectionManager();
            hitBoxCCM.addChatListener(this);
            hitBoxCCM.addFollowerListener(this);
            hitBoxCCM.start();
            
            if (getChatLogFile() != null) {
                new ChatLogRemovalTask(CHATLOG_REMOVAL_TIMER, getChatLogFile()).run();
            }
            
            LOG.info("main thread going to sleep");
            synchronized(this) { wait(); }
        } catch (IOException | IllegalAccessException | InstantiationException | InterruptedException ex) {
            LOG.error("", ex);
            throw new RuntimeException(ex);
        }
    }

    public void logLatestFollower(String followerName) {
        if (followerName.isEmpty()) {
            return;
        }
        if (config.filesOutputFolder == null) {
            return;
        }
        File latestFollowerFile = new File(config.filesOutputFolder, "latestFollower.txt");
        if (!latestFollowerFile.getParentFile().exists()) {
            latestFollowerFile.getParentFile().mkdirs();
        }
        try (OutputStream os = new FileOutputStream(latestFollowerFile, false)) {
            try (OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8")) {
                osw.append("Latest Follower: " + followerName + System.lineSeparator());
            }
        } catch (IOException ex) {
            LOG.error("", ex);
        }
        playSound(config.newFollowerSound);
    }

    private File getChatLogFile() {
        if (config.filesOutputFolder == null) {
            return null;
        }
        return new File(config.filesOutputFolder, "chat.log");
    }
    
    public void logChatMessage(String name, String text) {
        File chatLogFile = getChatLogFile();
        if (chatLogFile == null) {
            return;
        }
        if (!chatLogFile.getParentFile().exists()) {
            chatLogFile.getParentFile().mkdirs();
        }
        try (OutputStream os = new FileOutputStream(chatLogFile, true)) {
            try (OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8")) {
                osw.append(name + ": " + text + System.lineSeparator());
            }
        } catch (IOException ex) {
            LOG.error("", ex);
        }
        playSound(config.chatSound);
    }

    @Override
    public void onChatMessage(String name, String message) {
        LOG.info("chat: " + name + ": " + message);
        logChatMessage(name, message);
    }

    @Override
    public void onFollow(String name) {
        LOG.info("follow: " + name);
        logLatestFollower(name);
    }

    @Override
    public void onUnfollow(String name) {
    }

}
