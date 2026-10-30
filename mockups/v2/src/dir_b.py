# -*- coding: utf-8 -*-
from common import *

CSS = '''
:root{--bg:#F5F7FA;--acc:#2F6BFF;--tint:#E3EBFF;--t2:#58637A;--t3:#8E98AA}
.top{position:absolute;top:38px;left:16px;right:16px;height:52px;border-radius:26px;background:#fff;box-shadow:0 1px 2px rgba(31,50,100,.06),0 4px 14px rgba(31,50,100,.06);display:flex;align-items:center;padding:0 8px 0 18px;gap:12px;color:var(--t2);font-size:15px}
.top .ph{flex:1}
.av{width:36px;height:36px;border-radius:18px;background:linear-gradient(135deg,#2F6BFF,#7FA2FF);color:#fff;font-weight:700;font-size:15px;display:flex;align-items:center;justify-content:center}
.sec{display:flex;justify-content:space-between;align-items:baseline;padding:0 20px;margin:20px 0 10px}
.sec b{font-size:19px;font-weight:700;letter-spacing:-.2px}.sec span{font-size:13px;color:var(--acc);font-weight:600}
.cont{margin:0 16px;background:#fff;border-radius:28px;padding:14px;display:flex;gap:14px;box-shadow:0 1px 2px rgba(31,50,100,.05)}
.cover{border-radius:16px;position:relative;overflow:hidden;flex:none;display:flex;align-items:center;justify-content:center;font-weight:800;color:rgba(47,107,255,.28)}
.cover .ty{position:absolute;left:8px;top:8px;height:18px;border-radius:6px;background:rgba(255,255,255,.85);color:#2F6BFF;font-size:9.5px;font-weight:800;padding:0 6px;display:flex;align-items:center}
.cover .ln{position:absolute;left:10px;right:10px;bottom:12px;display:flex;flex-direction:column;gap:4px}
.cover .ln i{height:3px;border-radius:2px;background:rgba(47,107,255,.22);display:block}
.pbar{height:5px;border-radius:3px;background:#E3E8F1;overflow:hidden}.pbar i{display:block;height:100%;background:var(--acc);border-radius:3px}
.car{display:flex;gap:12px;padding:0 16px;overflow:hidden}
.car .c{width:112px;flex:none}
.car .c .t{font-size:13px;font-weight:600;line-height:18px;margin-top:8px;height:36px;overflow:hidden}
.car .c .s{font-size:11px;color:var(--t3);margin-top:2px}
.lrow{display:flex;gap:12px;align-items:center;padding:8px 20px}
.lrow .th{width:44px;height:58px;border-radius:10px;flex:none}
.nav{position:absolute;left:12px;right:12px;bottom:22px;height:66px;border-radius:33px;display:flex;align-items:center;padding:0 10px;z-index:30}
.nav .n{flex:1;height:50px;border-radius:25px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;font-size:10.5px;color:var(--t2)}
.nav .n.on{background:rgba(47,107,255,.12);color:#1D4FD6;font-weight:700}
.sfab{position:absolute;right:20px;bottom:104px;width:56px;height:56px;border-radius:18px;background:var(--acc);color:#fff;display:flex;align-items:center;justify-content:center;box-shadow:0 6px 16px rgba(47,107,255,.35);z-index:25}
/* reader */
.page{position:absolute;inset:0;background:#F7F9FC}
.rh{position:absolute;top:34px;left:0;right:0;text-align:center;font-size:11.5px;color:var(--t3);letter-spacing:.5px}
.txt{position:absolute;top:70px;left:0;right:0;padding:0 28px}
.txt h1{font-size:25px;line-height:36px;font-weight:700;margin-bottom:6px}
.txt .mt{font-size:12px;color:var(--t3);margin-bottom:16px}
.txt p{font-size:17px;line-height:30.5px;color:#1B2332;margin-bottom:12px;text-align:justify}
.txt h2{font-size:19px;font-weight:700;line-height:28px;margin:18px 0 8px}
.txt blockquote{border-left:3px solid var(--acc);padding-left:14px;font-size:16.5px;line-height:28px;color:#33405A;margin-bottom:14px}
.txt ol{list-style:none;counter-reset:c;margin-bottom:12px}.txt li{counter-increment:c;font-size:17px;line-height:30.5px;display:flex;gap:10px}.txt li:before{content:counter(c)".";color:var(--acc);font-weight:700;width:18px}
.foot{position:absolute;bottom:22px;left:28px;right:28px;display:flex;justify-content:space-between;font-size:11px;color:var(--t3)}
.hair{position:absolute;bottom:0;left:0;height:2px;width:42%;background:var(--acc)}
.sel{background:rgba(47,107,255,.22);border-radius:3px}
.selbar{position:absolute;left:52px;top:268px;height:50px;border-radius:25px;padding:0 6px;display:flex;align-items:center;z-index:30;gap:0}
.selbar .a{height:40px;padding:0 13px;display:flex;align-items:center;gap:6px;font-size:13.5px;font-weight:500;color:#1E2636;border-radius:20px}
.selbar .a.p{background:var(--acc);color:#fff;font-weight:600}
.hdl{position:absolute;width:14px;height:14px;border-radius:0 50% 50% 50%;background:var(--acc);transform:rotate(45deg);z-index:25}
.tbar{position:absolute;top:0;left:0;right:0;height:92px;padding:34px 8px 0;display:flex;align-items:center;z-index:30;background:rgba(255,255,255,.7);backdrop-filter:blur(22px) saturate(1.6);border-bottom:.5px solid rgba(255,255,255,.9);box-shadow:0 4px 18px rgba(31,50,100,.08)}
.bbar{position:absolute;left:10px;right:10px;bottom:20px;border-radius:30px;padding:14px 12px 10px;z-index:30}
.scrub{position:relative;height:30px;margin:0 8px}
.scrub .tr{position:absolute;left:0;right:0;top:13px;height:5px;border-radius:3px;background:#DCE3EE}
.scrub .fl{position:absolute;left:0;top:13px;height:5px;border-radius:3px;background:var(--acc);width:42%}
.scrub .tk{position:absolute;top:10px;width:2px;height:11px;border-radius:1px;background:#fff}
.scrub .hd{position:absolute;left:42%;top:5px;width:20px;height:20px;border-radius:10px;background:#fff;border:5px solid var(--acc);margin-left:-10px;box-shadow:0 2px 6px rgba(20,40,100,.3)}
.tools{display:flex;margin-top:6px}.tools .a{flex:1;height:56px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;font-size:11px;color:#2A3446}
.tools .a.pr{color:#fff}.tools .a.pr .b{background:var(--acc);}
.tools .b{width:48px;height:30px;border-radius:15px;display:flex;align-items:center;justify-content:center}
.sheet{position:absolute;left:0;right:0;bottom:0;background:#fff;border-radius:28px 28px 0 0;z-index:50;padding-bottom:36px}
.menu{position:absolute;right:10px;top:92px;width:214px;background:#fff;border-radius:18px;padding:6px;z-index:50;box-shadow:0 12px 36px rgba(20,30,60,.22)}
.mi{height:44px;border-radius:12px;display:flex;align-items:center;gap:14px;padding:0 12px;font-size:14.5px;color:#1E2636}.mi svg{color:var(--t2)}.mi.d,.mi.d svg{color:#D13A3A}
.mg{height:1px;background:#EBEEF4;margin:4px 8px}
.lab{font-size:13px;font-weight:600;color:var(--t2)}
.th4{display:flex;gap:12px}.th4 i{flex:1;height:58px;border-radius:16px;border:1px solid #D9DFEA;display:flex;flex-direction:column;align-items:center;justify-content:center;font-style:normal;font-size:19px;font-weight:700;gap:0;position:relative}
.th4 i small{font-size:10.5px;font-weight:500;color:inherit;opacity:.75}
.th4 i.on{outline:2.5px solid var(--acc);outline-offset:2px}
.step{display:flex;align-items:center;height:46px;border-radius:23px;background:#EEF2F8;padding:0 6px;justify-content:space-between}
.step .btn{width:40px;height:34px;border-radius:17px;background:#fff;display:flex;align-items:center;justify-content:center;font-size:17px;font-weight:600;box-shadow:0 1px 2px rgba(0,0,0,.08)}
.step .v{font-size:14px;font-weight:700}.step .v small{color:var(--t2);font-weight:500;font-size:12px;margin-left:6px}
.fc{height:34px;border-radius:10px;border:1px solid #D5DBE6;padding:0 14px;display:flex;align-items:center;gap:6px;font-size:13.5px}
.fc.on{background:#DCE6FF;border-color:transparent;color:#173A9A;font-weight:600}
.cg{display:flex;gap:2px;padding:0 16px}
.cg span{flex:1;height:40px;display:flex;align-items:center;justify-content:center;gap:5px;font-size:13.5px;background:#EEF2F8;color:#2A3446}
.cg span:first-child{border-radius:20px 8px 8px 20px}.cg span:last-child{border-radius:8px 20px 20px 8px}.cg span.on{background:var(--acc);color:#fff;font-weight:600;border-radius:20px}
.pv{flex:none;width:122px;height:162px;border-radius:14px;position:relative;overflow:hidden;box-shadow:0 4px 14px rgba(31,50,100,.16)}
.ed{position:absolute;top:96px;left:0;right:0;padding:6px 20px;font-size:15.5px;line-height:25px;color:#1E2636}
.ed .m{color:#2F6BFF}.ed b{font-weight:700}.l{min-height:25px;white-space:pre-wrap}
.caret{display:inline-block;width:2px;height:20px;background:var(--acc);vertical-align:-4px;margin-left:1px}
'''

COVERS = ['linear-gradient(145deg,#E8EFFF,#C9D9FF)','linear-gradient(145deg,#EEF2F8,#D5DFF0)','linear-gradient(145deg,#E3ECFF,#BFD1FA)','linear-gradient(145deg,#F0F4FB,#D6E0F3)']

def cover(d, w, h, i=0, fs=44):
    return (f'<div class="cover" style="width:{w}px;height:{h}px;background:{COVERS[i%4]};font-size:{fs}px"><span class="ty">{d["ty"]}</span>{d["ch"]}'
            f'<span class="ln"><i style="width:70%"></i><i style="width:50%"></i></span></div>')

def nav(active=0):
    it = [('home','home_f','阅读'),('library','lib_f','书库'),('user','user_f','我的')]
    h = ''.join(f'<div class="n {"on" if i==active else ""}">{svg(f if i==active else o,23)}{l}</div>' for i,(o,f,l) in enumerate(it))
    return f'<div class="nav glass">{h}</div>'

def home():
    D = DOCS
    cars = ''.join(f'<div class="c">{cover(D[i],112,150,i)}<div class="t">{D[i]["t"]}</div><div class="s">{D[i]["src"].replace(" · 公众号","")} · {D[i]["ago"]}</div></div>' for i in [1,2,4,5])
    body = f'''
<div class="top">{svg("search",22)}<span class="ph">在书库中搜索</span><div class="av">斯</div></div>
<div class="abs" style="top:100px;left:0;right:0">
<div class="sec" style="margin-top:10px"><b>继续阅读</b></div>
<div class="cont">{cover(D[0],92,124,0,40)}<div style="flex:1;min-width:0;display:flex;flex-direction:column;justify-content:space-between">
<div><div style="font-size:11.5px;color:var(--t3)">微信公众号 · 刚刚打开</div><div style="font-size:17px;font-weight:700;line-height:24px;margin-top:4px">{D[0]["t"]}</div></div>
<div><div class="pbar"><i style="width:42%"></i></div><div class="row" style="justify-content:space-between;margin-top:8px"><span style="font-size:12px;color:var(--t2)">已读 42% · 还剩约 2 分钟</span></div></div></div></div>
<div class="sec"><b>最近添加</b><span>全部</span></div>
<div class="car">{cars}</div>
<div class="sec" style="margin-top:14px"><b>收藏</b><span>全部</span></div>
<div class="lrow"><div class="th" style="background:{COVERS[2]}"></div><div style="flex:1"><div style="font-size:14.5px;font-weight:600">{D[3]["t"]}</div><div style="font-size:11.5px;color:var(--t3);margin-top:3px">文件 · 昨天 · 2 分钟</div></div><div style="color:#2F6BFF">{svg("bookmark_f",20)}</div></div>
</div>
<div class="sfab">{svg("plus",26,sw=1.9)}</div>
{nav(0)}'''
    return page(CSS, body)

def text_block(sel=False, sl_top=0):
    li=''.join(f'<li><span>{x}</span></li>' for x in READ_OL)
    p1 = READ_P1
    if sel:
        p1 = p1.replace('二十五分钟后才发现，注意力像被洗过一遍，','<span class="sel">二十五分钟后才发现，注意力像被洗过一遍，</span>')
    return f'''<div class="page"></div><div class="rh">三个小习惯</div>
<div class="txt serif" style="top:{70+sl_top}px"><h1>{READ_TITLE}</h1><div class="mt">微信公众号 · 10月30日 · 约 4 分钟</div>
<blockquote>{READ_QUOTE}</blockquote><p>{p1}</p><h2>{READ_H2}</h2><ol>{li}</ol><p>{READ_P2}</p><p>{READ_P3}</p></div>'''

def reader():
    body = text_block(True, -4) + f'''
<div class="hdl" style="left:75px;top:326px"></div><div class="hdl" style="left:121px;top:364px"></div>
<div class="selbar glass" style="top:244px;left:34px"><span class="a">复制</span><span class="a">{svg("highlight",17)}高亮</span><span class="a p">{svg("quotecard",17)}做成金句卡</span><span class="a" style="padding:0 8px">{svg("more",18)}</span></div>
<div class="foot"><span>周末读书笔记</span><span>42% · 还剩 2 分钟</span></div><div class="hair"></div>'''
    return page(CSS, body)

def chrome(menu=False):
    top = f'''<div class="tbar"><div class="ibtn">{svg("back",24)}</div><div style="flex:1;font-size:15px;font-weight:600;padding-left:4px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">周末读书笔记：慢下来…</div><div class="ibtn" style="color:#2F6BFF">{svg("bookmark_f",22)}</div><div class="ibtn">{svg("more",24)}</div></div>'''
    ticks=''.join(f'<i class="tk" style="left:{x}%"></i>' for x in (18,34,52,70,86))
    bot = f'''<div class="bbar glass"><div class="scrub"><div class="tr"></div><div class="fl"></div>{ticks}<div class="hd"></div></div>
<div class="row" style="justify-content:space-between;font-size:11.5px;color:var(--t2);padding:0 10px"><span>第 2 节 · 三个小习惯</span><span>还剩约 2 分钟</span></div>
<div class="tools"><div class="a"><div class="b">{svg("toc",22)}</div>目录</div><div class="a"><div class="b">{svg("aa",24)}</div>排版</div><div class="a"><div class="b">{svg("edit",21)}</div>编辑</div><div class="a pr"><div class="b">{svg("share",21)}</div><span style="color:#1D4FD6;font-weight:600">分享</span></div></div></div>'''
    mn = ''
    if menu:
        def mi(ic,t,d=False): return f'<div class="mi {"d" if d else ""}">{svg(ic,20)}{t}</div>'
        mn = f'''<div class="scrim" style="background:rgba(14,20,34,.14);z-index:35"></div><div class="menu">{mi("copy","复制全文")}{mi("rename","重命名")}{mi("info","文档信息")}<div class="mg"></div>{mi("link","在 Notion 打开")}{mi("history","历史版本")}<div class="mg"></div>{mi("trash","删除",True)}</div>'''
    return text_block(False,0)+top+bot+mn

def more(): return page(CSS, chrome(True))

def typo():
    body = text_block(False,-2)+f'''<div class="foot" style="z-index:1"><span></span><span></span></div>
<div class="sheet" style="box-shadow:0 -6px 30px rgba(20,30,60,.16)"><div class="hand"></div>
<div class="row" style="justify-content:space-between;padding:2px 24px 14px"><b style="font-size:19px">阅读样式</b><span style="font-size:13.5px;color:#2F6BFF;font-weight:600">重置</span></div>
<div style="padding:0 24px"><div class="th4"><i class="on" style="background:#F7F9FC">Aa<small>白</small></i><i style="background:#E8EEF8">Aa<small>雾蓝</small></i><i style="background:#F3EEDD">Aa<small>纸</small></i><i style="background:#141A24;color:#fff;border-color:#141A24">Aa<small>夜</small></i></div>
<div class="row" style="gap:8px;margin:16px 0 14px"><span class="lab" style="width:48px">字体</span><span class="fc">黑体</span><span class="fc on">{svg("check",15)}宋体</span><span class="fc">楷体</span></div>
<div class="row" style="gap:12px;margin-bottom:10px"><span class="lab" style="width:48px">字号</span><div class="step" style="flex:1"><div class="btn" style="font-size:13px">A</div><span class="v">17<small>标准</small></span><div class="btn" style="font-size:19px">A</div></div></div>
<div class="row" style="gap:12px;margin-bottom:10px"><span class="lab" style="width:48px">行距</span><div class="step" style="flex:1"><div class="btn">−</div><span class="v">1.8 ×<small>舒适</small></span><div class="btn">+</div></div></div>
<div class="row" style="gap:12px;margin-bottom:4px"><span class="lab" style="width:48px">段间距</span><div class="step" style="flex:1"><div class="btn">−</div><span class="v">0.6 em<small>紧凑</small></span><div class="btn">+</div></div></div>
</div></div>'''
    return page(CSS, body)

def share():
    sty = [('留白杂志',COVERS[0],True),('极细线框','linear-gradient(145deg,#fff,#EEF2F8)',False),('报纸排版','linear-gradient(145deg,#F2F0E8,#E3DFD0)',False),('渐变弥散','linear-gradient(145deg,#CFE0FF,#E8D9FF)',False),('撞色波普','linear-gradient(145deg,#FFE066,#FF8FA3)',False)]
    cards=''.join(f'<div style="flex:none;width:46px;text-align:center"><div style="width:46px;height:60px;border-radius:9px;background:{b};{"outline:2.5px solid #2F6BFF;outline-offset:2px;" if on else "border:1px solid #E1E6EF;"}"></div><div style="font-size:10px;margin-top:6px;color:{"#1D4FD6" if on else "#58637A"};font-weight:{700 if on else 500};white-space:nowrap;margin-left:-6px;margin-right:-6px">{n}</div></div>' for n,b,on in sty)
    def pv(i,w,h,body): return f'<div class="pv" style="width:{w}px;height:{h}px;background:{COVERS[0]}">{body}</div>'
    cov = '<div style="padding:16px 14px;height:100%;display:flex;flex-direction:column;justify-content:space-between"><div style="font-size:8px;color:#8E98AA;letter-spacing:1px">READING NOTE</div><div><div style="width:18px;height:2.5px;background:#E4572E;margin-bottom:8px"></div><div class="serif" style="font-size:15px;font-weight:700;line-height:21px">周末读书笔记：慢下来的力量</div><div style="font-size:8.5px;color:#58637A;margin-top:8px;line-height:12px">真正的效率，来自于对注意力的尊重。</div></div><div style="font-size:8px;color:#8E98AA">拾页</div></div>'
    bod = '<div style="padding:12px 11px;font-size:6.5px;line-height:10px;color:#2A3446"><div style="font-weight:800;font-size:9px;margin-bottom:5px">三个小习惯</div>1. 每天固定 25 分钟阅读<br>2. 读完写三句话总结<br>3. 周末回顾，挑一条付诸行动<br><br>习惯不需要宏大，只要足够小，小到今天就能开始。把「读完」换成「读进去」……</div>'
    body = text_block(False,0)+'<div class="scrim"></div>'+f'''
<div class="sheet" style="top:150px;padding-bottom:0"><div class="hand"></div>
<div class="row" style="justify-content:space-between;padding:2px 24px 12px"><b style="font-size:19px">导出与分享</b><div class="ibtn" style="width:32px;height:32px">{svg("close",20)}</div></div>
<div class="cg"><span class="on">{svg("cards",16)}小红书</span><span>{svg("long",16)}长图</span><span>{svg("notion",16)}Notion</span><span>{svg("file",16)}文件</span></div>
<div class="row" style="gap:12px;padding:18px 24px 0;justify-content:center"><div class="pv" style="background:#F6F1E7">{cov}</div><div class="pv" style="background:#F6F1E7;width:104px;height:138px;opacity:.9">{bod}</div><div class="pv" style="width:30px;height:138px;border-radius:12px 0 0 12px;background:#F6F1E7;opacity:.6;margin-right:-24px"></div></div>
<div class="row" style="justify-content:center;gap:6px;margin-top:12px;font-size:12px;color:#58637A">1 / 2 页 · 标题可修改</div>
<div style="padding:16px 24px 0"><div class="lab" style="margin-bottom:10px">样式</div><div class="row" style="gap:14px;overflow:hidden;padding-bottom:12px">{cards}</div>
<div class="lab" style="margin:6px 0 10px">比例</div></div>
<div class="cg" style="padding:0 24px"><span class="on">{svg("check",15)}3:4 · 1080×1440</span><span>9:16 · 1080×1920</span></div>
<div class="row" style="gap:10px;padding:20px 24px 0"><div style="flex:1;height:54px;border-radius:27px;background:#2F6BFF;color:#fff;font-weight:700;font-size:16px;display:flex;align-items:center;justify-content:center;gap:8px;box-shadow:0 6px 16px rgba(47,107,255,.32)">{svg("send",20)}发到小红书</div><div class="ibtn" style="width:54px;height:54px;border-radius:27px;border:1px solid #D5DBE6">{svg("download",22)}</div><div class="ibtn" style="width:54px;height:54px;border-radius:27px;border:1px solid #D5DBE6">{svg("more",22)}</div></div></div>'''
    return page(CSS, body)

def edit():
    tb = ''.join(f'<div class="ibtn" style="width:42px;height:42px">{svg(i,22)}</div>' for i in ['heading','bold','checklist','quote','code','link'])
    body = f'''
<div class="abs" style="top:30px;left:0;right:0;height:60px;display:flex;align-items:center;padding:0 12px;gap:8px;background:var(--bg)">
<div class="ibtn">{svg("close",24)}</div><div class="cg" style="flex:1;padding:0 4px"><span class="on" style="height:38px">{svg("edit",16)}编辑</span><span style="height:38px">{svg("eye",16)}预览</span></div><div style="font-size:15px;font-weight:700;color:#2F6BFF;padding:0 8px">保存</div></div>
<div class="ed mono" style="font-size:14.5px;top:98px">{md_src()}</div>
<div class="abs" style="left:0;right:0;bottom:262px;height:48px;background:#fff;border-top:1px solid #E3E8F0;z-index:30;display:flex;align-items:center;padding:0 8px;justify-content:space-between">
<div class="ibtn" style="width:42px;color:#B3BBC9">{svg("undo",21)}</div><div class="ibtn" style="width:42px;color:#B3BBC9">{svg("redo",21)}</div><div style="width:1px;height:22px;background:#D5DBE6"></div>{tb}</div>{kb()}'''
    return page(CSS, body)

SCREENS = dict(home=home, reader=reader, share=share, typography=typo, more=more, edit=edit)
