$(function(){
    $("#follBtn").click(follow);
});


function follow() {
    var btn = this;
    if($(btn).hasClass("fly-imActive")) {
        // 关注TA
        $.post(
            "/follow",
            {"entityType":3,"entityId":$('#entityId').val()},
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
            {"entityType":3,"entityId":$('#entityId').val()},
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