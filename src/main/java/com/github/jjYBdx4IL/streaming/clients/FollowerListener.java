package com.github.jjYBdx4IL.streaming.clients;

/**
 *
 * @author mark
 */
public interface FollowerListener {
    
    void onFollow(String name);
    void onUnfollow(String name);
    
}
