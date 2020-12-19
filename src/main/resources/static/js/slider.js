layui.config({
    base: 'layui/sliderVerify/'
}).use(['sliderVerify', 'jquery', 'form'], function() {
    var sliderVerify = layui.sliderVerify,
        $ = layui.jquery,
        form = layui.form;
    var slider = sliderVerify.render({
        elem: '#slider'
    })
    //监听提交
    form.on('submit(formDemo)', function(data) {
        if(slider.isOk()){//用于表单验证是否已经滑动成功
            layer.msg(JSON.stringify(data.field));
        }else{
            layer.msg("请先通过滑块验证");
        }
        return false;
    });

})