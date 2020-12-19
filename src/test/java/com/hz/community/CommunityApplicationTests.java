package com.hz.community;


import com.hz.community.dao.DiscussPostMapper;
import com.hz.community.dao.MessageMapper;
import com.hz.community.dao.UserMapper;
import com.hz.community.elasticsearch.DiscussPostRepository;
import com.hz.community.entity.DiscussPost;
import com.hz.community.entity.Message;
import com.hz.community.entity.User;
import com.hz.community.util.MailClient;
import com.hz.community.util.SensitiveFilter;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;


@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CommunityApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private MailClient mailClient;



    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void testInsert() {
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
    }

    @Test
    public void testInsertAll() {
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(165, 0, 100,0));

    }

    @Test
    public void testUpdate() {
        DiscussPost discussPost = discussPostMapper.selectDiscussPostById(231);
        discussPost.setContent("我是新人,使劲灌水");
        discussPostRepository.save(discussPost);
    }

    @Test
    public void testDelete() {
        discussPostRepository.deleteAll();

    }

    @Test
    public void testSearchByRepository() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0,10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        Page<DiscussPost> postPage =  discussPostRepository.search(searchQuery);
        System.out.println(postPage.getTotalElements());
        System.out.println(postPage.getTotalPages());
        System.out.println(postPage.getNumber());
        System.out.println(postPage.getSize());
        for (DiscussPost post : postPage) {
            System.out.println(post);
        }
    }


    @Test
    public void testString(){
        String key = "count";
        redisTemplate.opsForValue().set(key, 1);
        System.out.println(redisTemplate.opsForValue().get(key));
        System.out.println(redisTemplate.opsForValue().increment(key));
        System.out.println(redisTemplate.opsForValue().decrement(key));
    }
    @Test
    public void testMessageMapper(){
        List<Message> list = messageMapper.selectConversations(111, 0, 20);
        for (Message message:list){
            System.out.println(message);
        }

        int count = messageMapper.selectConversationCount(111);
        System.out.println(count);

        list = messageMapper.selectLetters("111_112",0,10);
        for (Message message:list){
            System.out.println(message);
        }
        count = messageMapper.selectLetterCount("111_112");
        System.out.println(count);

        count = messageMapper.selectLetterUnreadCount(131, "111_131");
        System.out.println(count);
    }

    @Test
    public void testSensitiveFilter(){
        String text = "可以💗赌💗博,可以💗嫖💗💗娼,哈哈!";
        text = sensitiveFilter.filter(text);
        System.out.println(text);

    }



    @Test
    public void testTextMail(){
        mailClient.sendMail("1941909674@qq.com","TEST","welcome");
    }

    @Test
    public void testSelectUser(){
        User user = userMapper.selectById(101);
        System.out.println(user);

        user = userMapper.selectByName("liubei");
        System.out.println(user);

        user = userMapper.selectByEmail("nowcoder1@sina.com");

        System.out.println(user);
    }

    @Test
    public void testInsertUser(){
        User user = new User();
        user.setUsername("test");
        user.setPassword("123");

        int rows = userMapper.insertUser(user);
        System.out.println(rows);
    }

    @Test
    public void testSelectPosts(){
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(149,0,10,0);

        for (DiscussPost post:list){
            System.out.println(post);
        }

        int rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(rows);
    }

}
