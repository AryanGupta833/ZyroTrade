package com.ZyroTrade.dailyPnlSnapshot;


import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;

@Configuration
@EnableScheduling
public class PnlScheduler {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyPnlSnapshotService dailyPnlSnapshotService;
    @Autowired
    private MeterRegistry meters;

    @Scheduled(cron = "0 * * * * ?")

    public void captureDailySnapShots(){
        List<User> users=userRepository.findAll();

        for(User user:users){
            try {
                dailyPnlSnapshotService.takeSnapShot(user.getUsername());
                meters.counter("daily_pnl.snapshot", "outcome", "success").increment();
            } catch (RuntimeException failure) {
                meters.counter("daily_pnl.snapshot", "outcome", "failure").increment();
            }
        }
    }
}
