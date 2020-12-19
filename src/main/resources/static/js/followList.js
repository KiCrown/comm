

function follow(e) {
    var data = e.getAttribute("data");
    var btn = $("#follBtn-"+data);
    if(btn.hasClass("fly-imActive")) {
        // 关注TA
        $.post(
            "/follow",
            {"entityType":3,"entityId":$('#entityId-'+data).val()},
            function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                    window.location.reload();
                }else {
                    layui.msg(data.msg);
                }
            }

        );
    } else {
        // 取消关注
        $.post(
            "/unfollow",
            {"entityType":3,"entityId":$('#entityId-'+data).val()},
            function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                    window.location.reload();
                }else {
                    layui.msg(data.msg);
                }
            }
        );
    }
}



function collect(e) {
    var data = e.getAttribute("data");
    var btn = $("#col-"+data);
    if(btn.hasClass("notcollect")) {
        // 收藏TA
        $.post(
            "/follow",
            {"entityType":4,"entityId":data},
            function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                    window.location.reload();
                }else {
                    layui.msg(data.msg);
                }
            }

        );
    } else {
        // 取消收藏
        $.post(
            "/unfollow",
            {"entityType":4,"entityId":data},
            function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                    window.location.reload();
                }else {
                    layui.msg(data.msg);
                }
            }
        );
    }
}