package com.hz.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.hz.community.entity.Message;
import com.hz.community.entity.Page;
import com.hz.community.entity.User;
import com.hz.community.service.DiscussPostService;
import com.hz.community.service.MessageService;
import com.hz.community.service.UserService;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.CommunityUtil;
import com.hz.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    private MessageService messageService;

    private HostHolder hostHolder;

    private UserService userService;

    private DiscussPostService discussPostService;

    @Autowired
    public MessageController(MessageService messageService, HostHolder hostHolder, UserService userService, DiscussPostService discussPostService) {
        this.messageService = messageService;
        this.hostHolder = hostHolder;
        this.userService = userService;
        this.discussPostService = discussPostService;
    }

    //私信列表
    @GetMapping("/letter/list")
    public String getLetterList(Model model, Page page) {
        User user = hostHolder.getUser();
        //分页信息
        page.setLimit(6);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));


        //会话列表
        List<Message> conversationList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null) {
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.findUserById(targetId));
                conversations.add(map);

            }

        }

        model.addAttribute("conversations", conversations);

        //查询未读信息数量
      /*  int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);*/

        return "/html/user/letter";
    }

    @GetMapping("/letter/detail/{conversationId}")
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));
        //私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);
        model.addAttribute("target", getLetterTarget(conversationId));
        //消息设置已读
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/html/user/letter-detail";

    }

    //获取from用户
    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id_0 = Integer.parseInt(ids[0]);
        int id_1 = Integer.parseInt(ids[1]);
        if (hostHolder.getUser().getId() == id_0) {
            return userService.findUserById(id_1);
        } else {
            return userService.findUserById(id_0);
        }
    }

    //获取未读消息
    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    @PostMapping("/letter/send")
    @ResponseBody
    public String sendLetter(String toName, String content) {
        User target = userService.findUserByName(toName);
        if (target == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在!");
        }
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);
        return CommunityUtil.getJSONString(0);
    }



    @GetMapping("/notice/comment")
    public String getCommentDetail(Page page, Model model) {
        User user = hostHolder.getUser();

        getNoticeList(user,model);
        page.setLimit(5);
        page.setPath("/notice/comment");
        page.setRows(messageService.findNoticeCount(user.getId(), "comment"));

        List<Message> noticeList = messageService.findNotices(user.getId(), "comment", page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoLost = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                //通知
                map.put("notice", notice);
                //内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("post",discussPostService.findDiscussPostById((Integer) data.get("postId")));

                noticeVoLost.add(map);
            }
        }
        model.addAttribute("notices", noticeVoLost);

        //设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        return "/html/user/comment-message";
    }



    @GetMapping("/notice/like")
    public String getLikeDetail(Page page, Model model) {
        User user = hostHolder.getUser();

        getNoticeList(user,model);
        page.setLimit(5);
        page.setPath("/notice/like");
        page.setRows(messageService.findNoticeCount(user.getId(), "like"));

        List<Message> noticeList = messageService.findNotices(user.getId(), "like", page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoLost = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                //通知
                map.put("notice", notice);
                //内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("post",discussPostService.findDiscussPostById((Integer) data.get("postId")));
                noticeVoLost.add(map);
            }
        }
        model.addAttribute("notices", noticeVoLost);

        //设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        return "/html/user/like-message";
    }


    @GetMapping("/notice/follow")
    public String getFollowDetail(Page page, Model model) {
        User user = hostHolder.getUser();

        getNoticeList(user,model);
        page.setLimit(5);
        page.setPath("/notice/follow");
        page.setRows(messageService.findNoticeCount(user.getId(), "follow"));

        List<Message> noticeList = messageService.findNotices(user.getId(), "follow", page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoLost = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                //通知
                map.put("notice", notice);
                //内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));

                noticeVoLost.add(map);
            }
        }
        model.addAttribute("notices", noticeVoLost);

        //设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        return "/html/user/follow-message";
    }




    private void getNoticeList(User user,Model model) {

        //查询评论类的通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);

        if (message != null) {
            Map<String, Object> messageVo = new HashMap<>();

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("count", count);
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("unreadCount", unreadCount);
            model.addAttribute("commentNotice", messageVo);
        }

        //查询点赞类的通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);

        if (message != null) {
            Map<String, Object> messageVo = new HashMap<>();

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVo.put("count", count);
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVo.put("unreadCount", unreadCount);
            model.addAttribute("likeNotice", messageVo);
        }

        //查询关注类的通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        if (message != null) {
            Map<String, Object> messageVo = new HashMap<>();
            messageVo.put("message", message);

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("count", count);
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("unreadCount", unreadCount);
            model.addAttribute("followNotice", messageVo);
        }


        //查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

    }

}
