
//展开二级评论
function collapseComments(e) {
    var id = e.getAttribute("data-id");
    var comments = $("#comment-"+id);
    var collapse = e.getAttribute("data-collapse");
    if(collapse){
        comments.removeClass("layui-show");
        e.removeAttribute("data-collapse");
        e.classList.remove("active");
    }else{
        comments.addClass("layui-show");
        e.setAttribute("data-collapse","layui-show");
        e.classList.add("active");
    }

}

//传递回复对象
function collapseSubComments(e) {
    var id = e.getAttribute("data-id");
    var name = e.getAttribute("data-name");
    var data = e.getAttribute("data");
    var isClick = e.getAttribute("isClick");
    var target = $('#target-' + data);
    var targetId = $('#targetId-'+ data);
    if (isClick) {
        target.attr('placeholder', "评论一下");
        e.removeAttribute("isClick");
        targetId.attr('value', 0);
    } else {
        target.attr('placeholder', "回复@" + name + ":");
        e.setAttribute("isClick",'1');
        targetId.attr('value', id);
    }


}




