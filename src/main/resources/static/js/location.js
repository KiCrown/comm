layui.extend({
    cascader: 'rc_cascader'
}).use(['jquery', 'cascader'], function () {
    var $ = layui.jquery, cascader = layui.cascader;

    cascader.render({
        elem: $('input[name=location]')[0],
        placeholder: str,
        options: [
            {
                label: '北京市',
                value: '北京市'
            },
            {
                label: '上海市',
                value: '上海市'
            },
            {
                label: '重庆市',
                value: '重庆市'
            },
            {
                label: '浙江省',
                value: '浙江省',
                children: [
                    {
                        label: '杭州',
                        value: '杭州'

                    },
                    {
                        label: '宁波',
                        value: '宁波'
                    },
                    {
                        label: '温州',
                        value: '温州'
                    },
                    {
                        label: '绍兴',
                        value: '绍兴'
                    },
                    {
                        label: '湖州',
                        value: '湖州'
                    },
                    {
                        label: '嘉兴',
                        value: '嘉兴'
                    },
                    {
                        label: '金华',
                        value: '金华'
                    },
                    {
                        label: '衢州',
                        value: '衢州'
                    },
                    {
                        label: '台州',
                        value: '台州'
                    },
                    {
                        label: '丽水',
                        value: '丽水'
                    },
                    {
                        label: '舟山',
                        value: '舟山'
                    }
                ]
            },
            {
                label: '江苏省',
                value: '江苏省',
                children: [
                    {
                        label: '南京市',
                        value: '南京市'

                    },
                    {
                        label: '无锡市',
                        value: '无锡市'
                    },
                    {
                        label: '徐州市',
                        value: '徐州市'
                    },
                    {
                        label: '常州市',
                        value: '常州市'
                    },
                    {
                        label: '苏州市',
                        value: '苏州市'
                    },
                    {
                        label: '南通市',
                        value: '南通市'
                    },
                    {
                        label: '连云港市',
                        value: '连云港市'
                    },
                    {
                        label: '淮安市',
                        value: '淮安市'
                    },
                    {
                        label: '盐城市',
                        value: '盐城市'
                    },
                    {
                        label: '扬州市',
                        value: '扬州市'
                    },
                    {
                        label: '镇江市',
                        value: '镇江市'
                    },
                    {
                        label: '泰州市',
                        value: '泰州市'
                    },
                    {
                        label: '宿迁市',
                        value: '宿迁市'
                    }
                ]
            },
            {
                label: '广东省',
                value: '广东省',
                children: [
                    {
                        label: '广州市',
                        value: '广州市'

                    },
                    {
                        label: '韶关市',
                        value: '韶关市'
                    },
                    {
                        label: '深圳市',
                        value: '深圳市'
                    },
                    {
                        label: '珠海市',
                        value: '珠海市'
                    },
                    {
                        label: '汕头市',
                        value: '汕头市'
                    },
                    {
                        label: '佛山市',
                        value: '佛山市'
                    },
                    {
                        label: '江门市',
                        value: '江门市'
                    },
                    {
                        label: '湛江市',
                        value: '湛江市'
                    },
                    {
                        label: '东莞市',
                        value: '东莞市'
                    },
                    {
                        label: '中山市',
                        value: '中山市'
                    },
                    {
                        label: '潮州市',
                        value: '潮州市'
                    },
                    {
                        label: '肇庆市',
                        value: '肇庆市'
                    },
                    {
                        label: '惠州市',
                        value: '惠州市'
                    }
                ]
            },
            {
                label: '福建省',
                value: '福建省',
                children: [
                    {
                        label: '福州市',
                        value: '福州市'

                    },
                    {
                        label: '厦门市',
                        value: '厦门市'
                    },
                    {
                        label: '漳州市',
                        value: '漳州市'
                    },
                    {
                        label: '泉州市',
                        value: '泉州市'
                    },
                    {
                        label: '三明市',
                        value: '三明市'
                    },
                    {
                        label: '莆田市',
                        value: '莆田市'
                    },
                    {
                        label: '南平市',
                        value: '南平市'
                    },
                    {
                        label: '龙岩市',
                        value: '龙岩市'
                    },
                    {
                        label: '宁德市',
                        value: '宁德市'
                    }
                ]
            },
            {
                label: '安徽省',
                value: '安徽省',
                children: [
                    {
                        label: '合肥',
                        value: '合肥'

                    },
                    {
                        label: '芜湖',
                        value: '芜湖'
                    },
                    {
                        label: '蚌埠',
                        value: '蚌埠'
                    },
                    {
                        label: '淮南',
                        value: '淮南'
                    },
                    {
                        label: '马鞍山',
                        value: '马鞍山'
                    },
                    {
                        label: '淮北',
                        value: '淮北'
                    },
                    {
                        label: '铜陵',
                        value: '铜陵'
                    },
                    {
                        label: '黄山',
                        value: '黄山'
                    },
                    {
                        label: '安庆',
                        value: '安庆'
                    },
                    {
                        label: '阜阳',
                        value: '阜阳'
                    },
                    {
                        label: '宿州',
                        value: '宿州'
                    },
                    {
                        label: '亳州',
                        value: '亳州'
                    },
                    {
                        label: '滁州',
                        value: '滁州'
                    }
                ]
            },
            {
                label: '河北省',
                value: '河北省'
            },
            {
                label: '山西省',
                value: '山西省'
            },
            {
                label: '辽宁省',
                value: '辽宁省'
            },
            {
                label: '吉林省',
                value: '吉林省'
            },
            {
                label: '黑龙江省',
                value: '黑龙江省'
            },
            {
                label: '江西省',
                value: '江西省'
            },
            {
                label: '山东省',
                value: '山东省'
            },
            {
                label: '河南省',
                value: '河南省'
            },
            {
                label: '湖北省',
                value: '湖北省'
            },
            {
                label: '湖南省',
                value: '湖南省'
            },
            {
                label: '海南省',
                value: '海南省'
            },
            {
                label: '四川省',
                value: '四川省'
            },
            {
                label: '贵州省',
                value: '贵州省'
            },
            {
                label: '云南省',
                value: '云南省'
            },
            {
                label: '陕西省',
                value: '陕西省'
            },
            {
                label: '台湾省',
                value: '台湾省'
            },
            {
                label: '甘肃省',
                value: '甘肃省'
            },
            {
                label: '青海省',
                value: '青海省'
            },
            {
                label: '香港',
                value: '香港'
            },
            {
                label: '澳门',
                value: '澳门'
            }

        ],
    })
});