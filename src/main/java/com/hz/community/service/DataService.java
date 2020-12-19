package com.hz.community.service;

import com.hz.community.entity.User;
import com.hz.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DataService {

    private RedisTemplate redisTemplate;

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    private UserService userService;

    @Autowired
    public DataService(RedisTemplate redisTemplate, UserService userService) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }


    //记录登录用户
    public void recordLogin(int userId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String redisKey = RedisKeyUtil.getLoginUser(df.format(new Date()));
                redisOperations.multi();
                redisOperations.opsForZSet().add(redisKey, userId, System.currentTimeMillis());
                return redisOperations.exec();
            }
        });
    }

    //记录签到
    public void recordSign(int userId, Date date) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String redisKey = RedisKeyUtil.getSign(userId, df.format(date));
                String signCountKey = RedisKeyUtil.getSignCount(userId);
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                c.add(Calendar.DAY_OF_MONTH, -1);
                Date yesterday = c.getTime();
                boolean hasSigned = hasSigned(userId, yesterday);
                redisOperations.multi();
                redisOperations.opsForZSet().add(redisKey, userId, System.currentTimeMillis());
                if (hasSigned) {
                    redisOperations.opsForValue().increment(signCountKey);
                } else {
                    redisOperations.opsForValue().set(signCountKey, 1);
                }
                return redisOperations.exec();
            }
        });
    }

    //查询用户是否签到
    public boolean hasSigned(int userId, Date date) {
        String redisKey = RedisKeyUtil.getSign(userId, df.format(date));
        return redisTemplate.opsForZSet().score(redisKey, userId) != null;
    }

    //查询用户连续签到天数
    public int getSignedCount(int userId) {
        String signCountKey = RedisKeyUtil.getSignCount(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(signCountKey);
        return count == null ? 0: count.intValue();
    }

    //查询登录的人
    public List<Map<String, Object>> findLogin(Date date) {
        String redisKey = RedisKeyUtil.getLoginUser(df.format(date));
        Set<Integer> targetIds =  redisTemplate.opsForZSet().reverseRange(redisKey, 0, 11);
        if (targetIds == null) {
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("userLogin",user);
            Double score = redisTemplate.opsForZSet().score(redisKey, targetId);
            map.put("loginTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

    //将指定IP计入UV
    public void recordUV(String ip) {
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    //统计指定日期范围内的UV
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        //整理该日期范围内的key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE, 1);
        }
        //合并这些数据
        String redisKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());
        //返回统计结果
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    //将指定用户计入DAU
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    //统计指定日期范围内的DAU
    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        //整理该日期范围内的key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE, 1);
        }
        //进行OR运算
        return (long)redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR, redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return redisConnection.bitCount(redisKey.getBytes());
            }
        });
    }


}
