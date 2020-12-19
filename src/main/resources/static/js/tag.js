var tag = xmSelect.render({
    el: '#tag',
    name: 'tag',
    filterable: true,
    layVerify: 'required',
    paging: true,
    pageSize: 5,
    max: 6,
    maxMethod(seles, item){
        layer.msg('最多可选6个', {icon: 5});
    },
    theme: {
        maxColor: 'orange',
    },
    toolbar: {
        show: true,
    },
    filterMethod: function(val, item, index, prop){
        if(val == item.value){//把value相同的搜索出来
            return true;
        }
        if(item.name.indexOf(val) != -1){//名称中包含的搜索出来
            return true;
        }
        return false;//不知道的就不管了
    },
    create: function(val, arr){
        if(arr.length === 0){
            return {
                name:  val,
                value: val
            }
        }
    },
    data: [
        {name: '测试', value: '测试'},
        {name: '讨论', value: '讨论'},
        {name: '新闻', value: '新闻'},
        {name: '教程', value: '教程'},
        {name: '灌水', value: '灌水'},
        {name: '笔记', value: '笔记'},
        {name: 'Spring', value: 'Spring'},
        {name: 'Java', value: 'Java'},
    ]
});
