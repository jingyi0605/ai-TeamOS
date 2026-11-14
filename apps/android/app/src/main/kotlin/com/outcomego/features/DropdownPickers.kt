package com.outcomego.features

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

enum class SelectorFieldStyle { Outlined, Settings }

data class BotAvatarShapeOption(val id: String, val labelEn: String, val labelZh: String)

data class BotAvatarColorOption(val hex: String, val labelEn: String, val labelZh: String)

data class VolcengineVoice(val id: String, val name: String, val group: String, val gender: String)

object PickerCatalogs {
    val robotAvatarShapes: List<BotAvatarShapeOption> = listOf(
        BotAvatarShapeOption("classic", "Classic", "经典"),
        BotAvatarShapeOption("goggles", "Goggles", "护目镜"),
        BotAvatarShapeOption("tv-head", "TV Head", "电视头"),
        BotAvatarShapeOption("terminal", "Terminal", "终端"),
        BotAvatarShapeOption("pod", "Pod", "太空舱"),
        BotAvatarShapeOption("hex-visor", "Hex Visor", "六角护目"),
        BotAvatarShapeOption("chip", "Chip", "芯片"),
        BotAvatarShapeOption("helmet", "Helmet", "头盔"),
        BotAvatarShapeOption("bulb", "Bulb", "灯泡"),
        BotAvatarShapeOption("owl", "Owl", "猫头鹰"),
        BotAvatarShapeOption("periscope", "Periscope", "潜望镜"),
        BotAvatarShapeOption("dual-screen", "Dual Screen", "双屏"),
    )

    val botAvatarColors: List<BotAvatarColorOption> = listOf(
        BotAvatarColorOption("#242424", "Black", "黑色"),
        BotAvatarColorOption("#A47952", "Brown", "棕色"),
        BotAvatarColorOption("#F23D52", "Red", "红色"),
        BotAvatarColorOption("#FF7A1A", "Orange", "橙色"),
        BotAvatarColorOption("#FF9E12", "Yellow", "黄色"),
        BotAvatarColorOption("#10B972", "Green", "绿色"),
        BotAvatarColorOption("#27BAAE", "Cyan", "青色"),
        BotAvatarColorOption("#4B8EFB", "Blue", "蓝色"),
        BotAvatarColorOption("#925DF2", "Violet", "紫色"),
        BotAvatarColorOption("#EF479B", "Magenta", "品红"),
        BotAvatarColorOption("#878787", "Gray", "灰色"),
    )

    val volcengineVoiceGroups: List<String> = listOf(
        "General",
        "Video voiceover",
        "Audiobook",
        "Roleplay",
        "Customer service",
        "Education",
        "Foreign language",
    )

    val volcengineVoices: List<VolcengineVoice> = listOf(
        VolcengineVoice("zh_female_vv_uranus_bigtts", "Vivi 2.0", "General", "Female"),
        VolcengineVoice("zh_female_xiaohe_uranus_bigtts", "小何 2.0", "General", "Female"),
        VolcengineVoice("zh_male_m191_uranus_bigtts", "云舟 2.0", "General", "Male"),
        VolcengineVoice("zh_male_taocheng_uranus_bigtts", "小天 2.0", "General", "Male"),
        VolcengineVoice("zh_male_sunwukong_uranus_bigtts", "猴哥 2.0", "Video voiceover", "Male"),
        VolcengineVoice("zh_female_xiaoxue_uranus_bigtts", "儿童绘本 2.0", "Audiobook", "Female"),
        VolcengineVoice("zh_male_dayi_uranus_bigtts", "大壹 2.0", "Video voiceover", "Male"),
        VolcengineVoice("zh_female_jitangnv_uranus_bigtts", "鸡汤女 2.0", "Video voiceover", "Female"),
        VolcengineVoice("zh_female_liuchangnv_uranus_bigtts", "流畅女声 2.0", "Video voiceover", "Female"),
        VolcengineVoice("zh_male_ruyayichen_uranus_bigtts", "儒雅逸辰 2.0", "Video voiceover", "Male"),
        VolcengineVoice("zh_female_wenroumama_uranus_bigtts", "温柔妈妈 2.0", "General", "Female"),
        VolcengineVoice("zh_female_zhishuaiyingzi_uranus_bigtts", "直率英子 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_male_silang_uranus_bigtts", "四郎 2.0", "Roleplay", "Male"),
        VolcengineVoice("zh_male_qingcang_uranus_bigtts", "擎苍 2.0", "Roleplay", "Male"),
        VolcengineVoice("zh_male_xionger_uranus_bigtts", "熊二 2.0", "Roleplay", "Male"),
        VolcengineVoice("zh_female_yingtaowanzi_uranus_bigtts", "樱桃丸子 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_male_naiqimengwa_uranus_bigtts", "奶气萌娃 2.0", "General", "Male"),
        VolcengineVoice("zh_female_popo_uranus_bigtts", "婆婆 2.0", "General", "Female"),
        VolcengineVoice("zh_male_baqiqingshu_uranus_bigtts", "霸气青叔 2.0", "Audiobook", "Male"),
        VolcengineVoice("zh_male_tangseng_uranus_bigtts", "唐僧 2.0", "Roleplay", "Male"),
        VolcengineVoice("zh_female_zhixingnv_uranus_bigtts", "知性女声 2.0", "General", "Female"),
        VolcengineVoice("zh_male_yuanboxiaoshu_uranus_bigtts", "渊博小叔 2.0", "General", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_shuanglangshaonian_tob", "爽朗少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("zh_male_liufei_uranus_bigtts", "刘飞 2.0", "General", "Male"),
        VolcengineVoice("zh_female_sophie_uranus_bigtts", "魅力苏菲 2.0", "General", "Female"),
        VolcengineVoice("zh_female_qingxinnvsheng_uranus_bigtts", "清新女声 2.0", "General", "Female"),
        VolcengineVoice("zh_female_cancan_uranus_bigtts", "知性灿灿 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_female_sajiaoxuemei_uranus_bigtts", "撒娇学妹 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_female_tianmeixiaoyuan_uranus_bigtts", "甜美小源 2.0", "General", "Female"),
        VolcengineVoice("zh_female_tianmeitaozi_uranus_bigtts", "甜美桃子 2.0", "General", "Female"),
        VolcengineVoice("zh_female_shuangkuaisisi_uranus_bigtts", "爽快思思 2.0", "General", "Female"),
        VolcengineVoice("zh_female_peiqi_uranus_bigtts", "佩奇猪 2.0", "Video voiceover", "Female"),
        VolcengineVoice("zh_female_linjianvhai_uranus_bigtts", "邻家女孩 2.0", "General", "Female"),
        VolcengineVoice("zh_male_shaonianzixin_uranus_bigtts", "少年梓辛 2.0", "General", "Male"),
        VolcengineVoice("zh_female_yingyujiaoxue_uranus_bigtts", "Tina老师 2.0", "Education", "Female"),
        VolcengineVoice("zh_female_kefunvsheng_uranus_bigtts", "暖阳女声 2.0", "Customer service", "Female"),
        VolcengineVoice("zh_female_mizai_uranus_bigtts", "黑猫侦探社咪仔 2.0", "Video voiceover", "Female"),
        VolcengineVoice("zh_female_meilinvyou_uranus_bigtts", "魅力女友 2.0", "General", "Female"),
        VolcengineVoice("en_male_tim_uranus_bigtts", "Tim", "Foreign language", "Male"),
        VolcengineVoice("en_female_dacey_uranus_bigtts", "Dacey", "Foreign language", "Female"),
        VolcengineVoice("en_female_stokie_uranus_bigtts", "Stokie", "Foreign language", "Female"),
        VolcengineVoice("zh_male_jieshuoxiaoming_uranus_bigtts", "解说小明 2.0", "General", "Male"),
        VolcengineVoice("zh_female_tvbnv_uranus_bigtts", "TVB女声 2.0", "General", "Female"),
        VolcengineVoice("zh_male_yizhipiannan_uranus_bigtts", "译制片男 2.0", "General", "Male"),
        VolcengineVoice("zh_female_qiaopinv_uranus_bigtts", "俏皮女声 2.0", "General", "Female"),
        VolcengineVoice("zh_male_linjiananhai_uranus_bigtts", "邻家男孩 2.0", "General", "Male"),
        VolcengineVoice("zh_male_ruyaqingnian_uranus_bigtts", "儒雅青年 2.0", "General", "Male"),
        VolcengineVoice("zh_male_wennuanahu_uranus_bigtts", "温暖阿虎 2.0", "General", "Male"),
        VolcengineVoice("zh_female_gaolengyujie_uranus_bigtts", "高冷御姐 2.0", "General", "Female"),
        VolcengineVoice("zh_male_aojiaobazong_uranus_bigtts", "傲娇霸总 2.0", "General", "Male"),
        VolcengineVoice("zh_male_lanyinmianbao_uranus_bigtts", "懒音绵宝 2.0", "Roleplay", "Male"),
        VolcengineVoice("zh_male_fanjuanqingnian_uranus_bigtts", "反卷青年 2.0", "General", "Male"),
        VolcengineVoice("zh_female_wenroushunv_uranus_bigtts", "温柔淑女 2.0", "General", "Female"),
        VolcengineVoice("zh_female_gufengshaoyu_uranus_bigtts", "古风少御 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_male_huolixiaoge_uranus_bigtts", "活力小哥 2.0", "General", "Male"),
        VolcengineVoice("zh_male_xuanyijieshuo_uranus_bigtts", "悬疑解说 2.0", "Audiobook", "Male"),
        VolcengineVoice("zh_female_mengyatou_uranus_bigtts", "萌丫头 2.0", "General", "Female"),
        VolcengineVoice("zh_female_tiexinnvsheng_uranus_bigtts", "贴心女声 2.0", "General", "Female"),
        VolcengineVoice("zh_female_jitangmei_uranus_bigtts", "鸡汤妹妹 2.0", "General", "Female"),
        VolcengineVoice("zh_male_cixingjieshuonan_uranus_bigtts", "磁性解说男声 2.0", "General", "Male"),
        VolcengineVoice("zh_male_liangsangmengzai_uranus_bigtts", "亮嗓萌仔 2.0", "General", "Male"),
        VolcengineVoice("zh_female_kailangjiejie_uranus_bigtts", "开朗姐姐 2.0", "General", "Female"),
        VolcengineVoice("zh_male_gaolengchenwen_uranus_bigtts", "高冷沉稳 2.0", "General", "Male"),
        VolcengineVoice("zh_male_shenyeboke_uranus_bigtts", "深夜播客 2.0", "General", "Male"),
        VolcengineVoice("zh_male_lubanqihao_uranus_bigtts", "鲁班七号 2.0", "Roleplay", "Male"),
        VolcengineVoice("zh_female_jiaochuannv_uranus_bigtts", "娇喘女声 2.0", "General", "Female"),
        VolcengineVoice("zh_female_linxiao_uranus_bigtts", "林潇 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_female_lingling_uranus_bigtts", "玲玲姐姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_female_chunribu_uranus_bigtts", "春日部姐姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_male_zhuangzhou_uranus_bigtts", "庄周 2.0", "Roleplay", "Male"),
        VolcengineVoice("zh_male_kailangdidi_uranus_bigtts", "开朗弟弟 2.0", "General", "Male"),
        VolcengineVoice("zh_male_zhubajie_uranus_bigtts", "猪八戒 2.0", "Roleplay", "Male"),
        VolcengineVoice("zh_female_ganmaodianyin_uranus_bigtts", "感冒电音姐姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_female_chanmeinv_uranus_bigtts", "谄媚女声 2.0", "General", "Female"),
        VolcengineVoice("zh_female_nvleishen_uranus_bigtts", "女雷神 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_female_qinqienv_uranus_bigtts", "亲切女声 2.0", "General", "Female"),
        VolcengineVoice("zh_male_kuailexiaodong_uranus_bigtts", "快乐小东 2.0", "General", "Male"),
        VolcengineVoice("zh_male_kailangxuezhang_uranus_bigtts", "开朗学长 2.0", "General", "Male"),
        VolcengineVoice("zh_male_youyoujunzi_uranus_bigtts", "悠悠君子 2.0", "General", "Male"),
        VolcengineVoice("zh_female_wenjingmaomao_uranus_bigtts", "文静毛毛 2.0", "General", "Female"),
        VolcengineVoice("zh_male_qingshuangnanda_uranus_bigtts", "清爽男大 2.0", "General", "Male"),
        VolcengineVoice("zh_male_yangguangqingnian_uranus_bigtts", "阳光青年 2.0", "General", "Male"),
        VolcengineVoice("zh_female_qingchezizi_uranus_bigtts", "清澈梓梓 2.0", "General", "Female"),
        VolcengineVoice("zh_female_tianmeiyueyue_uranus_bigtts", "甜美悦悦 2.0", "General", "Female"),
        VolcengineVoice("zh_female_xinlingjitang_uranus_bigtts", "心灵鸡汤 2.0", "General", "Female"),
        VolcengineVoice("zh_male_wenrouxiaoge_uranus_bigtts", "温柔小哥 2.0", "General", "Male"),
        VolcengineVoice("zh_female_roumeinvyou_uranus_bigtts", "柔美女友 2.0", "General", "Female"),
        VolcengineVoice("zh_male_dongfanghaoran_uranus_bigtts", "东方浩然 2.0", "General", "Male"),
        VolcengineVoice("zh_female_wenrouxiaoya_uranus_bigtts", "温柔小雅 2.0", "General", "Female"),
        VolcengineVoice("zh_male_tiancaitongsheng_uranus_bigtts", "天才童声 2.0", "General", "Male"),
        VolcengineVoice("zh_female_wuzetian_uranus_bigtts", "武则天 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_female_gujie_uranus_bigtts", "顾姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("zh_male_guanggaojieshuo_uranus_bigtts", "广告解说 2.0", "General", "Male"),
        VolcengineVoice("zh_female_shaoergushi_uranus_bigtts", "少儿故事 2.0", "Audiobook", "Female"),
        VolcengineVoice("ICL_uranus_en_female_charlie_tob", "Charlie 2.0", "Foreign language", "Female"),
        VolcengineVoice("ICL_uranus_en_male_ethan_tob", "Ethan 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_alastor_tob", "Alastor 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_chucky_tob", "Chucky 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_noah_tob", "Noah 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_jigsaw_tob", "Jigsaw 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_clown_man_tob", "Clown Man 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_cartoon_chef_tob", "Cartoon Chef 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_frosty_man_tob", "Frosty Man 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_the_grinch_tob", "The Grinch 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_kevin_mccallister_tob", "Kevin McCallister 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_michael_tob", "Michael 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_big_boogie_tob", "Big Boogie 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_xavier_tob", "Xavier 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_en_male_zayne_tob", "Zayne 2.0", "Foreign language", "Male"),
        VolcengineVoice("ICL_uranus_zh_female_kefuwanjun_tob", "客服婉君 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_yingxiaokefu_v2_tob", "营销小楠 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_aojiaonvyou_tob", "傲娇女友 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_aomanjiaosheng_tob", "傲慢娇声 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_xiemeinvwang_tob", "邪魅女王 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_bingjiaojiejie_tob", "病娇姐姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_bingjiaomengmei_tob", "病娇萌妹 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_bingruoshaonv_tob", "病弱少女 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_chengshuwenrou_tob", "成熟温柔 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_chengshujiejie_tob", "成熟姐姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_chunzhenshaonv_tob", "纯真少女 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_chunchenvsheng_tob", "纯澈女生 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_wumeikeren_tob", "妩媚可人 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_guaiqiaokeer_tob", "乖巧可儿 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_heainainai_tob", "和蔼奶奶 2.0", "Video voiceover", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_huopodiaoman_tob", "活泼刁蛮 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_huoponvhai_tob", "活泼女孩 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_jiaohannvwang_tob", "娇憨女王 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_jiaoruoluoli_tob", "娇弱萝莉 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_jiaxiaozi_tob", "假小子 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_jinglingxiangdao_tob", "精灵向导 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_kailangtingting_tob", "开朗婷婷 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_kefunvshengwenhecuishou_tob", "外呼凌凌 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_kaixinxiaohong_tob", "开心小鸿 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_keainvsheng_tob", "可爱女生 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_lingdongxinxin_tob", "灵动欣欣 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_linjuayi_tob", "邻居阿姨 2.0", "Video voiceover", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_tianmeijiaoqiao_tob", "甜美娇俏 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_qinglenggaoya_tob", "清冷高雅 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_lixingyuanzi_tob", "理性圆子 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_xingganmeihuo_tob", "性感魅惑 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_nuanxinqianqian_tob", "暖心茜茜 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_nuanxinxuejie_tob", "暖心学姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_qingtianmeimei_tob", "清甜莓莓 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_qingtiantaotao_tob", "清甜桃桃 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_qingxixiaoxue_tob", "清晰小雪 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_qingxinshaonv_tob", "倾心少女 2.0", "Video voiceover", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_rouguhunshi_tob", "柔骨魂师 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_ruanmengtangtang_tob", "软萌糖糖 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_ruanmengtuanzi_tob", "软萌团子 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_tianmeihuopo_tob", "甜美活泼 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_tianmeixiaoju_tob", "甜美小橘 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_tianmeixiaoyu_tob", "甜美小雨 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_tiaopigongzhu_tob", "调皮公主 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_tiexinnvyou_tob", "贴心女友 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_wenrounvshen_tob", "温柔女神 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_wenrouwenya_tob", "温柔文雅 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_zhixinjiejie_tob", "知心姐姐 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_wumeiyujie_tob", "妩媚御姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_yuanqitianmei_tob", "元气甜妹 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_xiemeiyujie_tob", "邪魅御姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_xingganyujie_tob", "性感御姐 2.0", "Roleplay", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_xiuliqianqian_tob", "秀丽倩倩 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_tiexinguimi_tob", "贴心闺蜜 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_tiexinmeimei_tob", "贴心妹妹 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_wenroubaiyueguang_tob", "温柔白月光 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_chuliannvyou_tob", "初恋女友 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_zhixingwenwan_tob", "知性温婉 2.0", "General", "Female"),
        VolcengineVoice("ICL_uranus_zh_male_aoqilingren_tob", "傲气凌人 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_anrenqinzhu_tob", "黯刃秦主 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_aojiaogongzi_tob", "傲娇公子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_aojiaojingying_tob", "傲娇精英 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_aomanqingnian_tob", "傲慢青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_aomanshaoye_tob", "傲慢少爷 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_zhenbiandiyu_tob", "枕边低语 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_badaoshaoye_tob", "霸道少爷 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_badaozongcai_tob", "霸道总裁 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_bingjiaobailian_tob", "病娇白莲 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_bingjiaodidi_tob", "病娇弟弟 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_bingjiaogege_tob", "病娇哥哥 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_bingjiaonanyou_tob", "病娇男友 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_bingjiaoshaonian_tob", "病娇少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_bingruogongzi_tob", "病弱公子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_bingruoshaonian_tob", "病弱少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_bujiqingnian_tob", "不羁青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_chunhoudiyin_tob", "醇厚低音 2.0", "Video voiceover", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_paoxiaoxiaoge_tob", "咆哮小哥 2.0", "Video voiceover", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_yangyang_tob", "炀炀 2.0", "General", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_chanruoshaoye_tob", "孱弱少爷 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_chengshuzongcai_tob", "成熟总裁 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_chenwenmingzai_tob", "沉稳明仔 2.0", "Customer service", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qingyisugan_tob", "清逸苏感 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_chunzhenxuedi_tob", "纯真学弟 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_cixingnansang_tob", "磁性男嗓 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_cujingnansheng_tob", "醋精男生 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_cujingnanyou_tob", "醋精男友 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_diyinchenyu_tob", "低音沉郁 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_fengfashaonian_tob", "风发少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_ruyagongzi_tob", "儒雅公子 2.0", "Audiobook", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_fuheigongzi_tob", "腹黑公子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_ganjingshaonian_tob", "干净少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_gaolengzongcai_tob", "高冷总裁 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_guaogongzi_tob", "孤傲公子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_gugaogongzi_tob", "孤高公子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_guiyishenmi_tob", "诡异神秘 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_guzhibingjiao_tob", "固执病娇 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_hanhoudunshi_tob", "憨厚敦实 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_huoliqingnian_tob", "活力青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_huoponanyou_tob", "活泼男友 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_huoposhuanglang_tob", "活泼爽朗 2.0", "General", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_huzishushu_tob", "胡子叔叔 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_jijiazhineng_tob", "机甲智能 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_jingyingqingnian_tob", "精英青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_junyigongzi_tob", "俊逸公子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_kailangqingkuai_tob", "开朗轻快 2.0", "General", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_kailangqingnian_tob", "开朗青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lanyincaohunshi_tob", "蓝银草魂师 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lengaozongcai_tob", "冷傲总裁 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lengdanshuli_tob", "冷淡疏离 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lengjungaozhi_tob", "冷峻高智 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lengjunshangsi_tob", "冷峻上司 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lengkugege_tob", "冷酷哥哥 2.0", "General", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lenglianxiongzhang_tob", "冷脸兄长 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lenglianxueba_tob", "冷脸学霸 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lengmonanyou_tob", "冷漠男友 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lengmoxiongzhang_tob", "冷漠兄长 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lingyunqingnian_tob", "凌云青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qinglengjingui_tob", "清冷矜贵 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_lvchaxiaoge_tob", "绿茶小哥 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_mengdongqingnian_tob", "懵懂青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_menyoupingxiaoge_tob", "闷油瓶小哥 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_xiaozhangxiaoge_tob", "嚣张小哥 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_nianrennanyou_tob", "粘人男友 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_neiliancaijun_tob", "内敛才俊 2.0", "Audiobook", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_nuanxintitie_tob", "暖心体贴 2.0", "General", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_pianpiangongzi_tob", "翩翩公子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_chenwenyouya_tob", "沉稳优雅 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qingsexiaosheng_tob", "青涩小生 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qingseqingnian_tob", "青涩青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qingshuangshaonian_tob", "清爽少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qingxinbobo_tob", "清新波波 2.0", "Customer service", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qinqieqingnian_tob", "亲切青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qinqiexiaozhuo_tob", "亲切小卓 2.0", "Customer service", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qinglangwenrun_tob", "清朗温润 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_rexueshaonian_tob", "热血少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_ruyacaijun_tob", "儒雅才俊 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_ruyajunzi_tob", "儒雅君子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_ruyazongcai_tob", "儒雅总裁 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_sajiaonansheng_tob", "撒娇男生 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_sajiaonanyou_tob", "撒娇男友 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_sajiaonianren_tob", "撒娇粘人 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_satuoqingnian_tob", "洒脱青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_shaonianjiangjun_tob", "少年将军 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_shenchenzongcai_tob", "深沉总裁 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_jilingxiaohuo_tob", "机灵小伙 2.0", "General", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_shenmifashi_tob", "神秘法师 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_shuaizhenxiaohuo_tob", "率真小伙 2.0", "General", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_shuanglangxiaoyang_tob", "爽朗小阳 2.0", "Customer service", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_dichenqianquan_tob", "低沉缱绻 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_siwenqingnian_tob", "斯文青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_tianxinanyou_tob", "甜系男友 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_tiexinnanyou_tob", "贴心男友 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_wenrounantongzhuo_tob", "温柔男同桌 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_wenrounanyou_tob", "温柔男友 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_wenrouxuezhang_tob", "温柔学长 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_wenrunxuezhe_tob", "温润学者 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_wenshunshaonian_tob", "温顺少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_guayanxiaoge_tob", "寡言小哥 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_xiaohouye_tob", "小侯爷 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_naiqixiaosheng_tob", "奶气小生 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_xiaosasuixing_tob", "潇洒随性 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_wenrouneilian_tob", "温柔内敛 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_xuebanantongzhuo_tob", "学霸男同桌 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_xuebatongzhuo_tob", "学霸同桌 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_yangguangyangyang_tob", "阳光洋洋 2.0", "Customer service", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_wennuanshaonian_tob", "温暖少年 2.0", "Audiobook", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_yiqishaonian_tob", "意气少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_younidashu_tob", "油腻大叔 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_youmodaye_tob", "幽默大爷 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_youmoshushu_tob", "幽默叔叔 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_youroubangzhu_tob", "优柔帮主 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_yourougongzi_tob", "优柔公子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_yuanqishaonian_tob", "元气少年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_zhangjianjunzi_tob", "仗剑君子 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_zhangjianxiake_tob", "仗剑侠客 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_zhengzhiqingnian_tob", "正直青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_zhishuaiqingnian_tob", "直率青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_zhongerqingnian_tob", "中二青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_zifuqingnian_tob", "自负青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_zixinqingnian_tob", "自信青年 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_tiancaitongzhuo_tob", "天才同桌 2.0", "Roleplay", "Male"),
        VolcengineVoice("ICL_uranus_zh_male_qingxinmumu_tob", "清新沐沐 2.0", "Customer service", "Male"),
        VolcengineVoice("ICL_uranus_zh_female_wenwanshanshan_tob", "温婉珊珊 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_reqingaina_tob", "热情艾娜 2.0", "Customer service", "Female"),
        VolcengineVoice("ICL_uranus_zh_female_qingyingduoduo_tob", "轻盈朵朵 2.0", "Customer service", "Female"),
    )

    fun knownTimeZones(include: String? = null): List<String> {
        val base = TimeZone.getAvailableIDs()
            .asSequence()
            .filter { id ->
                id == "UTC" || (id.contains('/') && !id.startsWith("SystemV/") && !id.startsWith("Etc/"))
            }
            .toMutableSet()
        if (!include.isNullOrBlank()) {
            base.add(include.trim())
        }
        return base.sorted()
    }

    fun formatTimeZoneDisplay(zoneId: String): String = zoneId.replace('_', ' ')

    fun formatCurrentTimeInZone(zoneId: String, nowMs: Long = System.currentTimeMillis()): String {
        val formatter = DateFormat.getTimeInstance(DateFormat.SHORT)
        formatter.timeZone = TimeZone.getTimeZone(zoneId)
        return formatter.format(Date(nowMs))
    }

    fun filterTimeZones(zones: List<String>, query: String): List<String> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return zones
        return zones.filter { zone ->
            zone.lowercase().contains(trimmed) || formatTimeZoneDisplay(zone).lowercase().contains(trimmed)
        }
    }

    fun minuteOptions(currentMinute: Int): List<Int> =
        ((0 until 60 step 5) + currentMinute.coerceIn(0, 59)).distinct().sorted()

    fun timeOptions(currentMinutes: Int): List<Int> =
        ((0 until 24 * 60 step 15) + currentMinutes.coerceIn(0, 24 * 60 - 1)).distinct().sorted()

    fun formatHourlyMinute(minute: Int): String = ":%02d".format(minute.coerceIn(0, 59))

    fun formatClockTime(minutesAfterMidnight: Int): String {
        val clamped = minutesAfterMidnight.coerceIn(0, 24 * 60 - 1)
        return "%02d:%02d".format(clamped / 60, clamped % 60)
    }

    fun weekdayOptions(): List<Int> = (1..7).toList()

    fun weekdayLabel(day: Int, useChinese: Boolean): String = when (day) {
        1 -> if (useChinese) "周一" else "Monday"
        2 -> if (useChinese) "周二" else "Tuesday"
        3 -> if (useChinese) "周三" else "Wednesday"
        4 -> if (useChinese) "周四" else "Thursday"
        5 -> if (useChinese) "周五" else "Friday"
        6 -> if (useChinese) "周六" else "Saturday"
        7 -> if (useChinese) "周日" else "Sunday"
        else -> day.toString()
    }

    fun monthDayOptions(): List<Int> = (1..31).toList()

    fun baseIntervalAmounts(unit: RoutineSchedule.IntervalUnit): List<Int> = when (unit) {
        RoutineSchedule.IntervalUnit.Minutes -> listOf(5, 10, 15, 20, 30, 45)
        RoutineSchedule.IntervalUnit.Hours -> listOf(1, 2, 3, 4, 6, 8, 12)
        RoutineSchedule.IntervalUnit.Days -> listOf(1, 2, 3, 7, 14, 30)
        RoutineSchedule.IntervalUnit.Weeks -> listOf(1, 2, 4)
    }

    fun defaultIntervalAmount(unit: RoutineSchedule.IntervalUnit): Int = when (unit) {
        RoutineSchedule.IntervalUnit.Minutes -> 30
        RoutineSchedule.IntervalUnit.Hours, RoutineSchedule.IntervalUnit.Days, RoutineSchedule.IntervalUnit.Weeks -> 1
    }

    fun intervalAmountOptions(unit: RoutineSchedule.IntervalUnit, currentAmount: Int): List<Int> =
        (baseIntervalAmounts(unit) + currentAmount.coerceAtLeast(1)).distinct().sorted()

    fun changeIntervalUnit(schedule: RoutineSchedule, unit: RoutineSchedule.IntervalUnit): RoutineSchedule {
        val adjustedAmount = if (schedule.intervalAmount in baseIntervalAmounts(unit)) {
            schedule.intervalAmount
        } else {
            defaultIntervalAmount(unit)
        }
        return schedule.copy(intervalUnit = unit, intervalAmount = adjustedAmount)
    }

    fun intervalUnitLabel(unit: RoutineSchedule.IntervalUnit, useChinese: Boolean): String = when (unit) {
        RoutineSchedule.IntervalUnit.Minutes -> if (useChinese) "分钟" else "minutes"
        RoutineSchedule.IntervalUnit.Hours -> if (useChinese) "小时" else "hours"
        RoutineSchedule.IntervalUnit.Days -> if (useChinese) "天" else "days"
        RoutineSchedule.IntervalUnit.Weeks -> if (useChinese) "周" else "weeks"
    }

    fun findVolcengineVoice(id: String): VolcengineVoice? {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return null
        return volcengineVoices.firstOrNull { it.id == trimmed }
    }

    fun filterVolcengineVoices(query: String): List<VolcengineVoice> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return volcengineVoices
        return volcengineVoices.filter { voice ->
            voice.name.lowercase().contains(trimmed) ||
                voice.id.lowercase().contains(trimmed) ||
                voice.group.lowercase().contains(trimmed)
        }
    }

    fun volcengineGroupLabel(group: String, useChinese: Boolean): String = if (!useChinese) group else when (group) {
        "General" -> "通用"
        "Video voiceover" -> "视频配音"
        "Audiobook" -> "有声书"
        "Roleplay" -> "角色扮演"
        "Customer service" -> "客服"
        "Education" -> "教育"
        "Foreign language" -> "外语"
        else -> group
    }

    fun volcengineGenderLabel(gender: String, useChinese: Boolean): String = if (!useChinese) gender else when (gender) {
        "Female" -> "女声"
        "Male" -> "男声"
        else -> gender
    }
}

@Composable
fun SelectorTriggerField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    style: SelectorFieldStyle = SelectorFieldStyle.Outlined,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    when (style) {
        SelectorFieldStyle.Outlined -> {
            val activeTextColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            val activeLabelColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            val activeBorderColor = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
            Box(modifier = modifier) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = label?.let { { Text(it) } },
                    placeholder = placeholder?.let { { Text(it) } },
                    supportingText = supportingText?.let { { Text(it) } },
                    leadingIcon = leadingContent?.let { content -> { content() } },
                    trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = activeTextColor,
                        disabledBorderColor = activeBorderColor,
                        disabledLabelColor = activeLabelColor,
                        disabledPlaceholderColor = activeLabelColor,
                        disabledSupportingTextColor = activeLabelColor,
                        disabledLeadingIconColor = activeLabelColor,
                        disabledTrailingIconColor = activeLabelColor,
                    ),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = enabled, onClick = onClick),
                )
            }
        }
        SelectorFieldStyle.Settings -> {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onClick),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (leadingContent != null) {
                        leadingContent()
                    }
                    Text(
                        text = value.ifBlank { placeholder.orEmpty() },
                        color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun <T> DropdownSelectorField(
    selectedValue: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    style: SelectorFieldStyle = SelectorFieldStyle.Outlined,
    isSameValue: (T, T) -> Boolean = { a, b -> a == b },
    leadingContent: (@Composable (T) -> Unit)? = null,
    optionLeadingContent: (@Composable (T) -> Unit)? = leadingContent,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { isSameValue(it, selectedValue) } ?: selectedValue
    val displayText = optionLabel(selectedOption)

    Box(modifier = modifier) {
        SelectorTriggerField(
            value = displayText,
            onClick = { expanded = true },
            label = label,
            placeholder = placeholder,
            supportingText = supportingText,
            enabled = enabled,
            style = style,
            leadingContent = leadingContent?.let { content -> { content(selectedOption) } },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            options.forEach { option ->
                val isSelected = isSameValue(option, selectedValue)
                DropdownMenuItem(
                    text = { Text(optionLabel(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = optionLeadingContent?.let { content -> { content(option) } },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTimeZoneSelector(
    selectedTimeZone: String,
    onSelectTimeZone: (timeZone: String, overrideTimeZone: String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    overrideTimeZone: String? = selectedTimeZone,
    detectedTimeZone: String? = null,
    includeAutoDetect: Boolean = false,
    useChinese: Boolean = localizedText("en", "zh") == "zh",
    style: SelectorFieldStyle = SelectorFieldStyle.Outlined,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val deviceZone = detectedTimeZone?.takeIf { it.isNotBlank() }
        ?: TimeZone.getDefault().id.takeIf { it.isNotBlank() }
        ?: "UTC"
    val effectiveZone = selectedTimeZone.ifBlank { overrideTimeZone ?: deviceZone }
    val displayValue = if (includeAutoDetect && overrideTimeZone == null) {
        val zoneName = PickerCatalogs.formatTimeZoneDisplay(effectiveZone)
        if (useChinese) "自动检测（$zoneName）" else "Auto-detect ($zoneName)"
    } else {
        PickerCatalogs.formatTimeZoneDisplay(effectiveZone)
    }

    SelectorTriggerField(
        value = displayValue,
        onClick = { sheetOpen = true },
        modifier = modifier,
        label = label,
        style = style,
    )

    if (sheetOpen) {
        val allZones = remember(effectiveZone) { PickerCatalogs.knownTimeZones(effectiveZone) }
        var query by remember { mutableStateOf("") }
        val filteredZones = remember(allZones, query) { PickerCatalogs.filterTimeZones(allZones, query) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = label ?: if (useChinese) "时区" else "Time zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(if (useChinese) "搜索时区或城市" else "Search time zones or cities") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    } else null,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 440.dp),
                ) {
                    if (includeAutoDetect && query.isBlank()) {
                        item(key = "__auto__") {
                            val autoLabel = if (useChinese) {
                                "自动检测（${PickerCatalogs.formatTimeZoneDisplay(deviceZone)}）"
                            } else {
                                "Auto-detect (${PickerCatalogs.formatTimeZoneDisplay(deviceZone)})"
                            }
                            PickerOptionRow(
                                title = autoLabel,
                                subtitle = if (useChinese) "使用当前设备时区" else "Use this device's time zone",
                                trailingText = PickerCatalogs.formatCurrentTimeInZone(deviceZone),
                                selected = overrideTimeZone == null,
                                onClick = {
                                    sheetOpen = false
                                    onSelectTimeZone(deviceZone, null)
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                    if (filteredZones.isEmpty()) {
                        item(key = "__empty__") {
                            Text(
                                text = if (useChinese) "未找到匹配的时区" else "No matching time zones",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 20.dp),
                            )
                        }
                    } else {
                        items(filteredZones, key = { it }) { zone ->
                            val isSelected = if (includeAutoDetect) {
                                overrideTimeZone == zone
                            } else {
                                effectiveZone == zone
                            }
                            PickerOptionRow(
                                title = PickerCatalogs.formatTimeZoneDisplay(zone),
                                trailingText = PickerCatalogs.formatCurrentTimeInZone(zone),
                                selected = isSelected,
                                onClick = {
                                    sheetOpen = false
                                    onSelectTimeZone(zone, zone)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolcengineVoiceSelector(
    selectedVoiceId: String,
    automaticLabel: String,
    onSelectVoice: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    supportingText: String? = null,
    useChinese: Boolean = localizedText("en", "zh") == "zh",
    style: SelectorFieldStyle = SelectorFieldStyle.Outlined,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val matchedVoice = remember(selectedVoiceId) { PickerCatalogs.findVolcengineVoice(selectedVoiceId) }
    val displayValue = when {
        selectedVoiceId.isBlank() -> automaticLabel
        matchedVoice != null -> matchedVoice.name
        else -> selectedVoiceId
    }

    SelectorTriggerField(
        value = displayValue,
        onClick = { sheetOpen = true },
        modifier = modifier,
        label = label,
        supportingText = supportingText,
        style = style,
    )

    if (sheetOpen) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(query) { PickerCatalogs.filterVolcengineVoices(query) }
        val grouped = remember(filtered) {
            PickerCatalogs.volcengineVoiceGroups.mapNotNull { group ->
                val voices = filtered.filter { it.group == group }
                if (voices.isEmpty()) null else group to voices
            }
        }
        val trimmedSelected = selectedVoiceId.trim()
        val isCustomVoice = trimmedSelected.isNotEmpty() && matchedVoice == null
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = label ?: if (useChinese) "语音" else "Voice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(if (useChinese) "按名称或音色 ID 搜索" else "Search voice name or ID") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    } else null,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 460.dp),
                ) {
                    if (query.isBlank()) {
                        item(key = "__auto__") {
                            PickerOptionRow(
                                title = automaticLabel,
                                selected = trimmedSelected.isEmpty(),
                                onClick = {
                                    sheetOpen = false
                                    onSelectVoice("")
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        if (isCustomVoice) {
                            item(key = "__custom__") {
                                PickerOptionRow(
                                    title = trimmedSelected,
                                    subtitle = if (useChinese) "自定义音色 ID" else "Custom voice ID",
                                    selected = true,
                                    onClick = { sheetOpen = false },
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    if (grouped.isEmpty()) {
                        item(key = "__empty__") {
                            Text(
                                text = if (useChinese) "未找到匹配的音色" else "No matching voices",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 20.dp),
                            )
                        }
                    } else {
                        grouped.forEach { (group, voices) ->
                            item(key = "header_$group") {
                                Text(
                                    text = PickerCatalogs.volcengineGroupLabel(group, useChinese),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                )
                            }
                            items(voices, key = { it.id }) { voice ->
                                val gender = PickerCatalogs.volcengineGenderLabel(voice.gender, useChinese)
                                val subtitle = if (gender.isNotBlank()) "$gender · ${voice.id}" else voice.id
                                PickerOptionRow(
                                    title = voice.name,
                                    subtitle = subtitle,
                                    selected = trimmedSelected == voice.id,
                                    onClick = {
                                        sheetOpen = false
                                        onSelectVoice(voice.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailingText: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        } else if (!trailingText.isNullOrBlank()) {
            Spacer(Modifier.width(18.dp))
        }
    }
}
