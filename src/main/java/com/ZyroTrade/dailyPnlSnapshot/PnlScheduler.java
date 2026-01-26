package com.ZyroTrade.dailyPnlSnapshot;


import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableScheduling
public class PnlScheduler {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyPnlSnapshotService dailyPnlSnapshotService;

    @Scheduled(cron = "0 * * * * ?")

    public void captureDailySnapShots(){
        List<User> users=userRepository.findAll();

        for(User user:users){
            dailyPnlSnapshotService.takeSnapShot(user.getUsername());
        }
    }
}
