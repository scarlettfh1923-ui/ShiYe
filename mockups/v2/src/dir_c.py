# -*- coding: utf-8 -*-
from common import *

CSS = '''
:root{--bg:#F5F7FA;--acc:#2F6BFF;--t2:#58637A;--t3:#8E98AA}
.top{position:absolute;top:38px;left:14px;right:14px;height:54px;border-radius:27px;display:flex;align-items:center;padding:0 8px 0 6px;gap:6px;color:var(--t2);font-size:15px;z-index:20}
.top .ph{flex:1}
.av{width:34px;height:34px;border-radius:17px;background:linear-gradient(135deg,#2F6BFF,#7FA2FF);color:#fff;font-weight:700;font-size:14px;display:flex;align-items:center;justify-content:center}
.chips{position:absolute;top:104px;left:0;right:0;display:flex;gap:8px;padding:0 14px;overflow:hidden}
.ch{height:34px;border-radius:17px;border:1px solid #D9DFEA;background:#fff;padding:0 14px;display:flex;align-items:center;gap:5px;font-size:13px;color:#2A3446;flex:none}
.ch.on{background:#DCE6FF;border-color:transparent;color:#173A9A;font-weight:700}
.grid{position:absolute;top:152px;left:12px;right:12px;display:flex;gap:10px}.grid .col{flex:1;display:flex;flex-direction:column;gap:10px}
.card{background:#fff;border-radius:20px;padding:14px 14px 12px;border:1px solid rgba(214,222,236,.7);box-shadow:0 1px 2px rgba(31,50,100,.04);position:relative}
.card.sel{border:2px solid var(--acc);background:#F2F6FF}
.card .tg{display:flex;gap:6px;align-items:center;font-size:10.5px;color:var(--t3);margin-bottom:8px}
.card .tt{font-size:15px;font-weight:700;line-height:21px;margin-bottom:6px}
.card .sn{font-size:12.5px;line-height:19px;color:var(--t2);display:-webkit-box;-webkit-box-orient:vertical;overflow:hidden}
.card .ft{display:flex;justify-content:space-between;align-items:center;margin-top:10px;font-size:10.5px;color:var(--t3)}
.badge{height:17px;border-radius:5px;background:#EAF0FF;color:#2F6BFF;font-weight:800;font-size:9px;padding:0 5px;display:inline-flex;align-items:center}
.bar{position:absolute;left:0;right:0;bottom:0;height:78px;padding:6px 14px 0;display:flex;align-items:flex-start;justify-content:space-between;z-index:30;border-radius:24px 24px 0 0}
.bar .ibt{width:46px;height:46px;display:flex;align-items:center;justify-content:center;color:#3A4660}
.cfab{position:absolute;right:16px;bottom:34px;width:60px;height:60px;border-radius:20px;background:#fff;color:var(--acc);display:flex;align-items:center;justify-content:center;box-shadow:0 6px 18px rgba(31,50,100,.2);z-index:40;border:1px solid rgba(47,107,255,.15)}
.page{position:absolute;inset:0;background:#F5F7FA}
.txt{position:absolute;top:104px;left:0;right:0;padding:0 24px}
.txt h1{font-size:25px;line-height:35px;font-weight:700;margin-bottom:8px}
.txt .mt{font-size:12px;color:var(--t3);margin-bottom:16px;display:flex;gap:6px;align-items:center}
.txt p{font-size:16.5px;line-height:29px;color:#1B2332;margin-bottom:10px}
.txt h2{font-size:19px;font-weight:700;line-height:28px;margin:16px 0 6px}
.txt blockquote{border-left:3px solid var(--acc);padding-left:14px;font-size:16px;line-height:27px;color:#33405A;margin-bottom:12px}
.txt ol{list-style:none;counter-reset:c;margin-bottom:10px}.txt li{counter-increment:c;font-size:16.5px;line-height:29px;display:flex;gap:10px}.txt li:before{content:counter(c)".";color:var(--acc);font-weight:700;width:18px}
.rtop{position:absolute;top:34px;left:10px;right:10px;height:56px;border-radius:28px;display:flex;align-items:center;padding:0 6px;z-index:30}
.rdock{position:absolute;left:50%;transform:translateX(-50%);bottom:24px;height:64px;border-radius:32px;display:flex;align-items:center;padding:0 8px;gap:2px;z-index:30;white-space:nowrap}
.rdock .a{height:48px;min-width:48px;border-radius:24px;display:flex;align-items:center;justify-content:center;gap:6px;font-size:13.5px;font-weight:600;color:#2A3446;padding:0 12px}
.rdock .a.on{background:rgba(47,107,255,.14);color:#1D4FD6}
.rdock .a.p{background:var(--acc);color:#fff;padding:0 18px}
.sheet{position:absolute;left:0;right:0;bottom:0;background:#fff;border-radius:28px 28px 0 0;z-index:50;padding-bottom:34px;box-shadow:0 -6px 30px rgba(20,30,60,.16)}
.srow{display:flex;align-items:center;gap:16px;height:54px;padding:0 24px;font-size:15.5px;color:#1E2636}.srow svg{color:var(--t2)}.srow.d,.srow.d svg{color:#D13A3A}
.pop{position:absolute;left:14px;right:14px;bottom:100px;border-radius:26px;z-index:50;padding:16px 18px 18px}
.pop:after{content:"";position:absolute;bottom:-7px;left:118px;width:16px;height:16px;background:inherit;transform:rotate(45deg);border-radius:3px}
.sl{display:flex;align-items:center;gap:12px;height:40px}.sl .l{width:44px;font-size:13px;font-weight:600;color:#3A4660}.sl .t{flex:1;height:4px;border-radius:2px;background:rgba(40,60,110,.15);position:relative}.sl .t i{position:absolute;left:0;top:0;bottom:0;border-radius:2px;background:var(--acc)}.sl .t b{position:absolute;top:-8px;width:20px;height:20px;border-radius:10px;background:#fff;border:2px solid var(--acc);margin-left:-10px;box-shadow:0 2px 6px rgba(20,40,100,.25)}
.sl .v{width:48px;text-align:right;font-size:12.5px;font-weight:700;color:#1D4FD6}
.sw{display:flex;gap:10px;margin-bottom:10px}.sw i{flex:1;height:40px;border-radius:20px;border:1px solid #D5DBE6;font-style:normal;font-size:13px;font-weight:600;display:flex;align-items:center;justify-content:center}.sw i.on{outline:2.5px solid var(--acc);outline-offset:1.5px}
.xrow{display:flex;gap:14px;padding:14px 20px;align-items:center;background:#fff;border-radius:22px;margin:0 14px 10px;border:1px solid rgba(214,222,236,.7)}
.xth{width:56px;height:74px;border-radius:10px;flex:none;position:relative;overflow:hidden}
.xrow .t{font-size:16px;font-weight:700}.xrow .s{font-size:12.5px;color:var(--t2);margin-top:3px;line-height:18px}
.ed{position:absolute;top:150px;left:0;right:0;padding:0 22px;font-size:15.5px;line-height:25px;color:#1E2636}
.ed .m{color:#2F6BFF}.ed b{font-weight:700}.l{min-height:25px;white-space:pre-wrap}
.caret{display:inline-block;width:2px;height:20px;background:var(--acc);vertical-align:-4px;margin-left:1px}
'''
COVERS = ['linear-gradient(145deg,#E8EFFF,#C9D9FF)','linear-gradient(145deg,#EEF2F8,#D5DFF0)','linear-gradient(145deg,#E3ECFF,#BFD1FA)']

SN = {
'周末读书笔记：慢下来的力量':'我们习惯把效率理解成「更快」，可真正让人沉下来的，往往是「更慢」。上周我把手机放进抽屉，只留一本纸质书和一杯水……',
'网页剪藏：设计灵感':'好的界面不是堆出来的，而是减出来的。收集了 12 个克制、留白充足的移动端案例，关注层级、节奏与动效。',
'Q4 路线图评审会议纪要':'结论：① 离线阅读与导出优先；② 搜索推迟到 11 月；③ 设计走查本周五完成。待办：@林 补充指标口径。',
'产品周报 · 10 月第 4 周':'本周上线 v1.1：小红书图文、长图、Notion 同步。次日留存 +2.1%，导出完成率 63%。下周计划：编辑体验优化。',
'为什么我们总是读不完一本书':'读不完不是意志力问题，而是「开始成本」太高。把阅读拆成 10 分钟的单元，反而更容易坚持下去。',
'Kotlin 协程速查表':'launch / async / withContext 的区别；Flow 冷流与热流；结构化并发下的异常传播与取消。',
'京都五日行程清单':'Day1 伏见稻荷 · 锦市场；Day2 岚山 · 竹林小径；Day3 清水寺 · 祇园；Day4 奈良一日；Day5 伊根。'}
def card(d, sel=False, lines=4, pin=False):
    bm = f'<span style="color:#2F6BFF">{svg("bookmark_f",15)}</span>' if d['fav'] else ''
    pn = f'<span style="color:#58637A">{svg("pin",14)}</span>' if pin else ''
    ck = f'<div style="position:absolute;left:-7px;top:-7px;width:26px;height:26px;border-radius:13px;background:#2F6BFF;color:#fff;display:flex;align-items:center;justify-content:center;box-shadow:0 2px 6px rgba(47,107,255,.4)">{svg("check",16,sw=2.4)}</div>' if sel else ''
    return f'''<div class="card {"sel" if sel else ""}">{ck}<div class="tg"><span class="badge">{d["ty"]}</span><span>{d["src"].replace(" · 公众号","")}</span><span style="flex:1"></span>{pn}</div><div class="tt">{d["t"]}</div><div class="sn" style="-webkit-line-clamp:{lines}">{SN[d["t"]]}</div><div class="ft"><span>{d["ago"]} · {d["mins"]} 分钟</span>{bm}</div></div>'''

def home():
    D = DOCS
    chips = '<div class="ch on">全部</div><div class="ch">'+svg("bookmark",14)+'收藏</div><div class="ch">微信</div><div class="ch">网页</div><div class="ch">文件</div>'
    col1 = card(D[0],False,5,True)+card(D[2],False,3)+card(D[5],False,2)
    col2 = card(D[1],False,3)+card(D[3],False,4)+card(D[4],False,3)
    body = f'''
<div class="top glass">{'<div class="ibtn">'+svg("menu",22)+'</div>'}<span class="ph" style="padding-left:6px">搜索拾页</span><div class="ibtn">{svg("grid",21)}</div><div class="av">斯</div></div>
<div class="chips">{chips}</div>
<div class="grid"><div class="col">{col1}</div><div class="col">{col2}</div></div>
<div class="bar glass" style="background:rgba(255,255,255,.82)"><div class="row" style="gap:2px"><div class="ibt">{svg("clip",24)}</div><div class="ibt">{svg("link",24)}</div><div class="ibt">{svg("file",24)}</div><div class="ibt">{svg("edit",23)}</div></div></div>
<div class="cfab">{svg("plus",28,sw=1.9)}</div>'''
    return page(CSS, body)

def prose(sel=False):
    li=''.join(f'<li><span>{x}</span></li>' for x in READ_OL)
    return f'''<div class="page"></div><div class="txt"><h1>{READ_TITLE}</h1><div class="mt"><span class="badge">MD</span>微信公众号 · 10月30日 · 约 4 分钟</div>
<blockquote>{READ_QUOTE}</blockquote><p>{READ_P1}</p><h2>{READ_H2}</h2><ol>{li}</ol><p>{READ_P2}</p><p>{READ_P3}</p></div>'''

def rchrome(active=None):
    top = f'''<div class="rtop glass"><div class="ibtn">{svg("back",24)}</div><div style="flex:1"></div><div class="ibtn">{svg("search",22)}</div><div class="ibtn" style="color:#2F6BFF">{svg("bookmark_f",22)}</div><div class="ibtn">{svg("more",24)}</div></div>'''
    dock = f'''<div class="rdock glass"><div class="a">{svg("toc",22)}</div><div class="a {"on" if active=="aa" else ""}">{svg("aa",24)}</div><div class="a">{svg("edit",21)}</div><div class="a p">{svg("share",20)}分享</div></div>'''
    return top+dock

def reader(): return page(CSS, prose()+rchrome())

def typo():
    pop = f'''<div class="pop glass"><div class="sw"><i class="on" style="background:#F5F7FA">白</i><i style="background:#E8EEF8">雾蓝</i><i style="background:#F3EEDD">纸</i><i style="background:#141A24;color:#fff;border-color:#141A24">夜</i></div>
<div class="sw" style="margin-bottom:6px"><i class="on" style="background:rgba(255,255,255,.6)">黑体</i><i style="background:rgba(255,255,255,.4)" >宋体</i><i style="background:rgba(255,255,255,.4)">楷体</i></div>
<div class="sl"><span class="l">字号</span><div class="t"><i style="width:44%"></i><b style="left:44%"></b></div><span class="v">17</span></div>
<div class="sl"><span class="l">行距</span><div class="t"><i style="width:58%"></i><b style="left:58%"></b></div><span class="v">1.75×</span></div>
<div class="sl"><span class="l">段间距</span><div class="t"><i style="width:30%"></i><b style="left:30%"></b></div><span class="v">0.6em</span></div></div>'''
    return page(CSS, prose()+rchrome("aa")+pop)

def more():
    def r(ic,t,d=False): return f'<div class="srow {"d" if d else ""}">{svg(ic,22)}{t}</div>'
    body = prose()+'<div class="scrim"></div>'+f'''<div class="sheet"><div class="hand"></div>
<div style="padding:2px 24px 10px"><div style="font-size:17px;font-weight:700">周末读书笔记：慢下来的力量</div><div style="font-size:12px;color:#8E98AA;margin-top:4px">Markdown · 12.4 KB · 编辑于 今天 21:08</div></div>
<div style="height:1px;background:#EBEEF4;margin:4px 0"></div>
{r("bookmark_f","取消收藏")}{r("tag","加入合集…")}{r("pin","置顶")}{r("copy","复制全文")}{r("rename","重命名")}{r("history","历史版本")}{r("link","在 Notion 打开")}{r("trash","删除",True)}</div>'''
    return page(CSS, body)

def share():
    def x(bg,t,s,ic,sty):
        return f'<div class="xrow"><div class="xth" style="background:{bg}">{sty}</div><div style="flex:1"><div class="t">{t}</div><div class="s">{s}</div></div><div style="color:#8E98AA">{svg("chev",20)}</div></div>'
    xh='<div style="padding:10px 8px;font-size:5.5px;line-height:8px;color:#2A3446"><div style="width:10px;height:2px;background:#E4572E;margin-bottom:4px"></div><b class="serif" style="font-size:8px;line-height:10px;display:block">周末读书笔记</b><br>真正的效率，来自于对注意力的尊重。</div>'
    lg='<div style="padding:8px 7px;font-size:4.5px;line-height:7px;color:#2A3446"><b style="font-size:6px">周末读书笔记</b><br>我们习惯把效率理解成「更快」……<br><br><b>三个小习惯</b><br>1. 每天固定 25 分钟<br>2. 读完写三句话总结</div>'
    body = prose()+'<div class="scrim"></div>'+f'''<div class="sheet" style="padding-bottom:24px;background:#F5F7FA"><div class="hand"></div>
<div class="row" style="justify-content:space-between;padding:2px 24px 12px"><b style="font-size:19px">分享</b><span style="font-size:13px;color:#58637A">选一种方式，下一页再微调</span></div>
{x("#F6F1E7","小红书图文","7 套样式 · 3:4 / 9:16 · 约 2 页",0,xh)}
{x("#fff","长图","完整一张 · 可存相册或发送",0,lg).replace('class="xth" style="background:#fff"','class="xth" style="background:#fff;border:1px solid #E1E6EF"')}
{x("#EEF2F8","同步到 Notion","已连接 · 阅读笔记库",0,'<div style="display:flex;height:100%;align-items:center;justify-content:center;color:#2F6BFF">'+svg("notion",26)+'</div>')}
{x("#EEF2F8","发送文件","原样 .md · 其他应用打开",0,'<div style="display:flex;height:100%;align-items:center;justify-content:center;color:#2F6BFF">'+svg("file",26)+'</div>')}
</div>'''
    return page(CSS, body)

def edit():
    tb = ''.join(f'<div class="ibtn" style="width:44px;height:44px">{svg(i,22)}</div>' for i in ['heading','bold','checklist','quote','code','link'])
    body = f'''<div class="page"></div>
<div class="abs" style="top:34px;left:10px;right:10px;height:56px;display:flex;align-items:center;gap:4px"><div class="ibtn">{svg("back",24)}</div><div style="flex:1"></div><div class="ibtn">{svg("undo",22)}</div><div class="ibtn" style="color:#B3BBC9">{svg("redo",22)}</div><div class="ibtn" style="color:#2F6BFF">{svg("eye",23)}</div><div class="ibtn">{svg("more",24)}</div></div>
<div class="abs" style="top:92px;left:22px;right:22px"><div style="font-size:22px;font-weight:700">周末读书笔记：慢下来的力量</div><div style="font-size:11.5px;color:#8E98AA;margin-top:6px">Markdown · 已修改 · 自动保存</div></div>
<div class="ed mono" style="top:172px;font-size:14.5px">{md_src()}</div>
<div class="abs glass" style="left:12px;right:12px;bottom:276px;height:54px;border-radius:27px;z-index:30;display:flex;align-items:center;padding:0 6px;justify-content:space-between">{tb}<div class="ibtn" style="width:44px;height:44px;border-radius:22px;background:#2F6BFF;color:#fff">{svg("check",22,sw=2.2)}</div></div>{kb()}'''
    return page(CSS, body)

SCREENS = dict(home=home, reader=reader, share=share, typography=typo, more=more, edit=edit)
