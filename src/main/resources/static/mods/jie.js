/**

 @Name: 求解板块

 */
 
layui.define('fly', function(exports){

  var $ = layui.jquery;
  var layer = layui.layer;
  var util = layui.util;
  var laytpl = layui.laytpl;
  var form = layui.form;
  var fly = layui.fly;
  
  var gather = {}, dom = {
    jieda: $('#jieda')
    ,content: $('#L_content')
    ,jiedaCount: $('#jiedaCount')
  };

  //监听专栏选择
  form.on('select(column)', function(obj){
    var value = obj.value
    ,elemQuiz = $('#LAY_quiz')
    ,tips = {
      tips: 1
      ,maxWidth: 250
      ,time: 10000
    };
    elemQuiz.addClass('layui-hide');
    if(value === '0'){
      layer.tips('请选择相应的专栏', obj.othis, tips);
      elemQuiz.removeClass('layui-hide');
    } else if(value === '99'){
      layer.tips('系统会对【分享】类型的帖子予以飞吻奖励，但我们需要审核，通过后方可展示', obj.othis, tips);
    }
  });

  //提交回答
  fly.form['/jie/reply/'] = function(data, required){
    var tpl = '<li>\
      <div class="detail-about detail-about-reply">\
        <a class="fly-avatar" href="/u/{{ layui.cache.user.uid }}" target="_blank">\
          <img src="{{= d.user.avatar}}" alt="{{= d.user.username}}">\
        </a>\
        <div class="fly-detail-user">\
          <a href="/u/{{ layui.cache.user.uid }}" target="_blank" class="fly-link">\
            <cite>{{d.user.username}}</cite>\
          </a>\
        </div>\
        <div class="detail-hits">\
          <span>刚刚</span>\
        </div>\
      </div>\
      <div class="detail-body jieda-body photos">\
        {{ d.content}}\
      </div>\
    </li>'
    data.content = fly.content(data.content);
    laytpl(tpl).render($.extend(data, {
      user: layui.cache.user
    }), function(html){
      required[0].value = '';
      dom.jieda.find('.fly-none').remove();
      dom.jieda.append(html);
      
      var count = dom.jiedaCount.text()|0;
      dom.jiedaCount.html(++count);
    });
  };

  //求解管理
  gather.jieAdmin = {
    //拉黑帖子
    del: function(div){
      layer.confirm('确认拉黑该帖子么？', function(index){
        layer.close(index);
        $.post(
            "/discuss/delete",
            {"id": $("#postId").val()},
            function (data) {
              data = $.parseJSON(data);
              if (data.code == 0) {
                layer.msg("拉黑成功", {
                  icon: 6,
                  time: 1000,
                });
               window.location.href = "/index";
              } else {
                layer.msg(data.msg);
              }
            }
        );
      });
    }
    
    //设置置顶状态
    ,top: function(div){
      var othis = $(this);
      var rank = othis.attr('rank');
      if (rank == 1) {
        layer.confirm('确认置顶帖子么？', function(index){
          layer.close(index);
          $.post(
              "/discuss/top",
              {"id": $("#postId").val(),"rank":rank},
              function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                  layer.msg("置顶成功", {
                    icon: 6,
                    time: 1000,
                  });
                  window.location.reload();
                } else {
                  layer.msg(data.msg);
                }
              }
          );
        });
      }
      if (rank == 0) {
        layer.confirm('确认取消置顶帖子么？', function(index){
          layer.close(index);
          $.post(
              "/discuss/top",
              {"id": $("#postId").val(),"rank":rank},
              function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                  layer.msg("取消置顶成功", {
                    icon: 6,
                    time: 1000,
                  });
                  window.location.reload();
                } else {
                  layer.msg(data.msg);
                }
              }
          );
        });
      }
    }
    //设置加精状态
    ,won: function(div){
      var othis = $(this);
      var rank = othis.attr('rank');
      if (rank == 1) {
        layer.confirm('确认加精帖子么？', function(index){
          layer.close(index);
          $.post(
              "/discuss/wonderful",
              {"id": $("#postId").val(),"rank":rank},
              function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                  layer.msg("加精成功", {
                    icon: 6,
                    time: 1000,
                  });
                  window.location.reload();
                } else {
                  layer.msg(data.msg);
                }
              }
          );
        });
      }
      if (rank == 0) {
        layer.confirm('确认取消加精帖子么？', function(index){
          layer.close(index);
          $.post(
              "/discuss/wonderful",
              {"id": $("#postId").val(),"rank":rank},
              function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                  layer.msg("取消加精成功", {
                    icon: 6,
                    time: 1000,
                  });
                  window.location.reload();
                } else {
                  layer.msg(data.msg);
                }
              }
          );
        });
      }

    }
    //点赞
    ,zan: function (span) {
      var othis = $(this);
      var entityId = othis.attr("entityId");
      var entityUserId = othis.attr("userId");
      var postId = othis.attr("postId");
      var ok = othis.hasClass('zan');
      $.post(
          "/like",
          {"entityType":1,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},
          function (data) {
            data = $.parseJSON(data);
            if(data.code == 0){
              othis[ok ? 'removeClass' : 'addClass']('zan');
              othis.find('span').html(data.likeCount);
            }else {
              layer.msg(data.msg);
            }
          }
      );

    }
    //收藏
    ,col: function(div){
      var othis = $(this);
      var postId = othis.attr("postId");
      var ok = othis.hasClass('notcollect');
      // 关注TA
      if(ok) {
        $.post(
            "/follow",
            {"entityType": 4, "entityId": postId},
            function (data) {
              data = $.parseJSON(data);
              if (data.code == 0) {
                window.location.reload();
              } else {
                layui.msg(data.msg);
              }
            }
        );
      } else {
        // 取消关注
        $.post(
            "/unfollow",
            {"entityType": 4, "entityId": postId},
            function (data) {
              data = $.parseJSON(data);
              if (data.code == 0) {
                window.location.reload();
              } else {
                layui.msg(data.msg);
              }
            }
        );
      }
    }
  };

  $('body').on('click', '.jie-admin', function(){
    var othis = $(this), type = othis.attr('type');
    gather.jieAdmin[type] && gather.jieAdmin[type].call(this, othis.parent());
  });

  //异步渲染
  var asyncRender = function(){
    var div = $('.fly-admin-box'), jieAdmin = $('#LAY_jieAdmin');
    //查询帖子是否收藏
    if(jieAdmin[0] && layui.cache.user.uid != -1){
      fly.json('/collection/find/', {
        cid: div.data('id')
      }, function(res){
        jieAdmin.append('<span class="layui-btn layui-btn-xs jie-admin '+ (res.data.collection ? 'layui-btn-danger' : '') +'" type="collect" data-type="'+ (res.data.collection ? 'remove' : 'add') +'">'+ (res.data.collection ? '取消收藏' : '收藏') +'</span>');
      });
    }
  }();

  //解答操作
  gather.jiedaActive = {
    //点赞
    zan: function(li){
      var othis = $(this)
      var ok = othis.hasClass('zanok');
      var entityId = othis.attr("entityId");
      var entityUserId = othis.attr("userId");
      var postId = othis.attr("postId");
      $.post(
          "/like",
          {"entityType":2,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},
          function (data) {
            data = $.parseJSON(data);
            if(data.code == 0){
              othis[ok ? 'removeClass' : 'addClass']('zanok');
              othis.find('em').html(data.likeCount);
            }else {
              layer.msg(data.msg);
            }
          }
      );
    }
    ,reply: function(li){ //回复
      var val = dom.content.val();
      var aite = '@'+ li.find('.fly-detail-user cite').text().replace(/\s/g, '');
      dom.content.focus()
      if(val.indexOf(aite) !== -1) return;
      dom.content.val(aite +' ' + val);
    }
    ,accept: function(li){ //采纳
      var othis = $(this);
      layer.confirm('是否采纳该回答为最佳答案？', function(index){
        layer.close(index);
        fly.json('/api/jieda-accept/', {
          id: li.data('id')
        }, function(res){
          if(res.status === 0){
            $('.jieda-accept').remove();
            li.addClass('jieda-daan');
            li.find('.detail-about').append('<i class="iconfont icon-caina" title="最佳答案"></i>');
          } else {
            layer.msg(res.msg);
          }
        });
      });
    }
    ,edit: function(li){ //编辑
      fly.json('/jie/getDa/', {
        id: li.data('id')
      }, function(res){
        var data = res.rows;
        layer.prompt({
          formType: 2
          ,value: data.content
          ,maxlength: 100000
          ,title: '编辑回帖'
          ,area: ['728px', '300px']
          ,success: function(layero){
            fly.layEditor({
              elem: layero.find('textarea')
            });
          }
        }, function(value, index){
          fly.json('/jie/updateDa/', {
            id: li.data('id')
            ,content: value
          }, function(res){
            layer.close(index);
            li.find('.detail-body').html(fly.content(value));
          });
        });
      });
    }
    ,del: function(li){ //删除
      layer.confirm('确认删除该回答么？', function(index){
        layer.close(index);
        fly.json('/api/jieda-delete/', {
          id: li.data('id')
        }, function(res){
          if(res.status === 0){
            var count = dom.jiedaCount.text()|0;
            dom.jiedaCount.html(--count);
            li.remove();
            //如果删除了最佳答案
            if(li.hasClass('jieda-daan')){
              $('.jie-status').removeClass('jie-status-ok').text('求解中');
            }
          } else {
            layer.msg(res.msg);
          }
        });
      });    
    }
  };

  $('.jieda-reply span').on('click', function(){
    var othis = $(this), type = othis.attr('type');
    gather.jiedaActive[type].call(this, othis.parents('li'));
  });


  //定位分页
  if(/\/page\//.test(location.href) && !location.hash){
    var replyTop = $('#flyReply').offset().top - 80;
    $('html,body').scrollTop(replyTop);
  }

  exports('jie', null);
});