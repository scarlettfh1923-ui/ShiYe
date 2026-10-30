# -*- coding: utf-8 -*-
from common import *

CSS = '''
:root{--bg:#F5F7FA;--acc:#2F6BFF;--tint:#DCE6FF;--cont:#EBEFF6;--t2:#58637A;--t3:#8E98AA}
.appsearch{position:absolute;left:16px;right:16px;top:40px;height:56px;border-radius:28px;background:var(--cont);display:flex;align-items:center;padding:0 8px 0 18px;gap:12px;color:var(--t2);font-size:15.5px}
.appsearch .ph{flex:1}
.chips{position:absolute;left:0;right:0;top:108px;display:flex;gap:8px;padding:0 16px;overflow:hidden}
.chip{height:34px;border-radius:10px;border:1px solid #D5DBE6;padding:0 14px;display:flex;align-items:center;gap:6px;font-size:13.5px;color:#2A3446;white-space:nowrap;background:transparent}
.chip.on{background:var(--tint);border-color:transparent;color:#173A9A;font-weight:600}
.glabel{font-size:12.5px;font-weight:600;color:var(--t2);padding:0 22px;margin:14px 0 8px;letter-spacing:.3px}
.group{padding:0 16px;display:flex;flex-direction:column;gap:2px}
.it{background:#fff;padding:12px 6px 12px 12px;display:flex;gap:12px;align-items:center;border-radius:8px}
.it.f{border-radius:22px 22px 8px 8px}.it.l{border-radius:8px 8px 22px 22px}.it.o{border-radius:22px}
.tile{width:52px;height:52px;border-radius:16px;display:flex;align-items:center;justify-content:center;flex:none;font-size:13px;font-weight:700}
.tile.md{background:#E3EBFF;color:#2F6BFF}.tile.html{background:#EDF0F6;color:#5A667E}
.it .b{flex:1;min-width:0}
.it .h{font-size:15.5px;font-weight:600;line-height:21px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.it .s{font-size:12.5px;color:var(--t2);line-height:18px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:1px}
.it .m{white-space:nowrap;font-size:11.5px;color:var(--t3);margin-top:3px;display:flex;gap:6px;align-items:center}
.nav{position:absolute;left:0;right:0;bottom:0;height:84px;background:#EDF1F8;display:flex;padding:10px 8px 0;z-index:30}
.nav .n{flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;font-size:11.5px;color:var(--t2)}
.nav .pill{width:60px;height:32px;border-radius:16px;display:flex;align-items:center;justify-content:center}
.nav .n.on .pill{background:var(--tint);color:#173A9A}.nav .n.on{color:#173A9A;font-weight:700}
.efab{position:absolute;right:16px;bottom:100px;height:56px;border-radius:18px;background:var(--acc);color:#fff;display:flex;align-items:center;gap:10px;padding:0 20px 0 16px;font-size:15.5px;font-weight:600;box-shadow:0 6px 18px rgba(47,107,255,.35);z-index:25}
.rtop{position:absolute;top:30px;left:0;right:0;height:56px;display:flex;align-items:center;padding:0 8px;z-index:30}
.prog{position:absolute;top:30px;left:0;height:2.5px;width:38%;background:var(--acc);z-index:35;border-radius:0 2px 2px 0}
.doc{position:absolute;top:92px;left:0;right:0;padding:0 22px}
.doc h1{font-size:26px;line-height:35px;font-weight:700;letter-spacing:-.2px}
.metaln{display:flex;align-items:center;gap:8px;margin:12px 0 16px;font-size:12.5px;color:var(--t3)}
.tag{height:22px;border-radius:7px;padding:0 8px;background:#E3EBFF;color:#2F6BFF;font-size:11px;font-weight:700;display:flex;align-items:center}
.q{border-left:3px solid var(--acc);padding:2px 0 2px 14px;font-size:16px;line-height:26px;color:#2A3446;margin-bottom:16px;background:linear-gradient(90deg,rgba(47,107,255,.06),transparent)}
.doc p{font-size:16.5px;line-height:29px;color:#1E2636;margin-bottom:11px}
.doc h2{font-size:20px;line-height:28px;font-weight:700;margin:20px 0 8px}
.doc ol{padding-left:0;list-style:none;counter-reset:c;margin-bottom:11px}
.doc li{counter-increment:c;font-size:16.5px;line-height:29px;display:flex;gap:10px}
.doc li:before{content:counter(c)".";color:var(--acc);font-weight:700;width:18px}
.ftool{position:absolute;bottom:26px;left:0;right:0;display:flex;justify-content:center;gap:8px;z-index:30;align-items:center}
.ftb{height:64px;border-radius:32px;padding:0 10px;display:flex;align-items:center;gap:2px}
.ftb .a{width:62px;height:52px;border-radius:26px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;font-size:10.5px;color:#2A3446}
.fabs{width:64px;height:64px;border-radius:22px;background:var(--acc);color:#fff;display:flex;align-items:center;justify-content:center;box-shadow:0 8px 20px rgba(47,107,255,.38)}
.sheet{position:absolute;left:0;right:0;bottom:0;background:#fff;border-radius:28px 28px 0 0;z-index:50;padding-bottom:34px}
.sheet h3{font-size:19px;font-weight:700;padding:6px 24px 0}
.sheet .sub{font-size:12.5px;color:var(--t3);padding:2px 24px 14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.tiles{display:grid;grid-template-columns:1fr 1fr;gap:10px;padding:0 16px}
.tl{background:var(--cont);border-radius:24px;padding:16px;height:118px;display:flex;flex-direction:column;justify-content:space-between}
.tl .ic{width:42px;height:42px;border-radius:21px;background:#fff;color:var(--acc);display:flex;align-items:center;justify-content:center}
.tl.pri{background:var(--tint)}.tl.pri .ic{background:var(--acc);color:#fff}
.tl .n{font-size:15.5px;font-weight:700}.tl .d{font-size:12px;color:var(--t2);margin-top:2px}
.tlrow{display:flex;gap:8px;padding:14px 16px 0}
.pillb{height:40px;border-radius:12px;border:1px solid #D5DBE6;display:flex;align-items:center;gap:8px;padding:0 14px;font-size:13.5px;color:#2A3446}
.menu{position:absolute;right:12px;top:84px;width:232px;background:#fff;border-radius:20px;padding:6px;z-index:50;box-shadow:0 12px 36px rgba(20,30,60,.22)}
.mi{height:46px;border-radius:14px;display:flex;align-items:center;gap:14px;padding:0 12px;font-size:14.5px;color:#1E2636}
.mi svg{color:var(--t2)}.mi.d{color:#D13A3A}.mi.d svg{color:#D13A3A}
.mg{height:6px}
.lab{font-size:13px;color:var(--t2);font-weight:600}
.sl{position:relative;height:34px;margin:6px 0 4px}
.sl .tr{position:absolute;left:0;right:0;top:10px;height:14px;border-radius:7px;background:#E6EBF4}
.sl .fl{position:absolute;left:0;top:10px;height:14px;border-radius:7px 0 0 7px;background:var(--acc)}
.sl .hd{position:absolute;top:3px;width:5px;height:28px;border-radius:3px;background:var(--acc);box-shadow:0 0 0 3px #fff}
.val{font-size:13px;font-weight:700;color:var(--acc)}
.cg{display:flex;gap:2px}
.cg span{flex:1;height:42px;display:flex;align-items:center;justify-content:center;gap:6px;font-size:14px;background:var(--cont);color:#2A3446}
.cg span:first-child{border-radius:21px 8px 8px 21px}.cg span:last-child{border-radius:8px 21px 21px 8px}
.cg span.on{background:var(--acc);color:#fff;font-weight:600;border-radius:21px}
.sw{display:flex;gap:14px}
.sw i{width:44px;height:44px;border-radius:22px;border:1px solid #D5DBE6;display:flex;align-items:center;justify-content:center;font-style:normal;font-size:15px;font-weight:700}
.sw i.on{outline:2.5px solid var(--acc);outline-offset:2px}
.ed{position:absolute;top:96px;left:0;right:0;padding:6px 20px;font-size:15.5px;line-height:25px;color:#1E2636}
.ed .m{color:#2F6BFF}.ed b{font-weight:700}
.caret{display:inline-block;width:2px;height:20px;background:var(--acc);vertical-align:-4px;margin-left:1px}
'''

def doc_row(d, pos):
    cls = {'f':'f','l':'l','o':'o','m':''}[pos]
    tile = f'<div class="tile md">MD</div>' if d['ty']=="MD" else f'<div class="tile html">{svg("globe",22)}</div>'
    bm = f'<div class="ibtn" style="color:#2F6BFF">{svg("bookmark_f",22)}</div>' if d['fav'] else f'<div class="ibtn" style="color:#8E98AA">{svg("bookmark",22)}</div>'
    cloud = f'<span style="color:#2F6BFF;display:flex">{svg("cloud",13)}</span>' if d['syn'] else ''
    return (f'<div class="it {cls}">{tile}<div class="b"><div class="h">{d["t"]}</div><div class="s">{d["sm"]}</div>'
            f'<div class="m"><span>{d["src"].replace(" · 公众号","")} · {d["ago"]} · {d["mins"]} 分钟</span>{cloud}</div></div>{bm}</div>')

def nav(active=0):
    items = [('library','lib_f','书库'),('bookmark','bm_f_nav','收藏'),('send','send_f','已发布'),('tune','tune_f','设置')]
    h=''
    for i,(o,f,l) in enumerate(items):
        on = 'on' if i==active else ''
        h += f'<div class="n {on}"><div class="pill">{svg(f if i==active else o,24)}</div>{l}</div>'
    return f'<div class="nav">{h}</div>'

def home():
    D = DOCS
    body = f'''
<div class="appsearch">{svg("search",22)}<span class="ph">搜索 24 篇文档</span><div class="ibtn" style="color:#2A3446">{svg("sort",22)}</div></div>
<div class="chips"><div class="chip on">{svg("check",16)}全部</div><div class="chip">Markdown</div><div class="chip">网页</div><div class="chip">未读</div></div>
<div class="abs" style="top:150px;left:0;right:0">
<div class="glabel">今天</div>
<div class="group">{doc_row(D[0],'f')}{doc_row(D[1],'l')}</div>
<div class="glabel">昨天</div>
<div class="group">{doc_row(D[2],'f')}{doc_row(D[3],'m')}{doc_row(D[4],'l')}</div>
</div>
<div class="efab">{svg("plus",24,sw=2)}导入</div>
{nav(0)}'''
    return page(CSS, body)

def reader_base(extra_cls=''):
    li = ''.join(f'<li><span>{x}</span></li>' for x in READ_OL)
    return f'''
<div class="prog"></div>
<div class="rtop"><div class="ibtn">{svg("back",24)}</div><div style="flex:1"></div><div class="ibtn" style="color:#2F6BFF">{svg("bookmark_f",23)}</div><div class="ibtn">{svg("more",24)}</div></div>
<div class="doc">
<h1>{READ_TITLE}</h1>
<div class="metaln"><span class="tag">MD</span><span>微信公众号 · 10月30日 · 约 4 分钟</span></div>
<div class="q">{READ_QUOTE}</div>
<p>{READ_P1}</p><h2>{READ_H2}</h2><ol>{li}</ol><p>{READ_P2}</p><p>{READ_P3}</p>
</div>'''

def ftool(show=True):
    if not show: return ''
    return f'''<div class="ftool"><div class="ftb glass">
<div class="a">{svg("toc",23)}目录</div><div class="a">{svg("aa",24)}排版</div><div class="a">{svg("edit",22)}编辑</div></div>
<div class="fabs">{svg("share",27)}</div></div>'''

def reader():
    return page(CSS, reader_base()+ftool())

def share():
    tiles = [
     ('cards','小红书图文','7 套样式 · 3:4 / 9:16',True),
     ('long','长图','完整一张 · 可存相册',False),
     ('notion','同步到 Notion','已连接 · 阅读笔记库',False),
     ('file','发送文件','原样 .md 文件',False)]
    th=''.join(f'<div class="tl {"pri" if p else ""}"><div class="ic">{svg(i,22)}</div><div><div class="n">{n}</div><div class="d">{d}</div></div></div>' for i,n,d,p in tiles)
    body = reader_base()+'<div class="scrim"></div>'+f'''
<div class="sheet"><div class="hand"></div><h3>分享与导出</h3><div class="sub">《{READ_TITLE}》</div>
<div class="tiles">{th}</div>
<div class="tlrow"><div class="pillb">{svg("copy",18)}复制全文</div><div class="pillb">{svg("share",18)}更多应用…</div></div></div>'''
    return page(CSS, body)

def typo():
    body = reader_base()+ftool(False)+f'''
<div class="sheet" style="box-shadow:0 -6px 30px rgba(20,30,60,.16);padding-bottom:40px">
<div class="hand"></div>
<div class="row" style="padding:2px 24px 10px;justify-content:space-between"><h3 style="padding:0">阅读样式</h3><span style="font-size:13.5px;color:#2F6BFF;font-weight:600">重置</span></div>
<div style="padding:0 24px">
<div class="row" style="gap:8px;margin-bottom:14px"><span class="pillb" style="height:34px">紧凑</span><span class="pillb" style="height:34px;background:#DCE6FF;border-color:transparent;color:#173A9A;font-weight:600">标准</span><span class="pillb" style="height:34px">舒展</span></div>
<div class="cg" style="margin-bottom:14px"><span class="on">{svg("check",17)}无衬线</span><span>宋体</span></div>
<div class="row" style="justify-content:space-between"><span class="lab">字号</span><span class="val">17</span></div><div class="sl"><div class="tr"></div><div class="fl" style="width:46%"></div><div class="hd" style="left:46%"></div></div>
<div class="row" style="justify-content:space-between"><span class="lab">行距</span><span class="val">1.75 ×</span></div><div class="sl"><div class="tr"></div><div class="fl" style="width:58%"></div><div class="hd" style="left:58%"></div></div>
<div class="row" style="justify-content:space-between"><span class="lab">段间距</span><span class="val">0.6 em</span></div><div class="sl"><div class="tr"></div><div class="fl" style="width:30%"></div><div class="hd" style="left:30%"></div></div>
<div class="sw" style="margin-top:12px"><i class="on" style="background:#F5F7FA">白</i><i style="background:#E8EEF8">雾</i><i style="background:#F3EEDD">纸</i><i style="background:#141A24;color:#fff">夜</i></div>
</div></div>'''
    return page(CSS, body)

def more():
    def mi(ic,t,d=False): return f'<div class="mi {"d" if d else ""}">{svg(ic,21)}{t}</div>'
    body = reader_base()+ftool()+'<div class="scrim" style="background:rgba(14,20,34,.18)"></div>'+f'''
<div class="menu">{mi("copy","复制全文")}{mi("rename","重命名")}{mi("info","文档信息")}<div class="mg"></div>
{mi("link","在 Notion 打开")}{mi("history","历史版本")}<div class="mg"></div>{mi("trash","删除",True)}</div>'''
    return page(CSS, body)

def edit():
    tb = ''.join(f'<div class="ibtn" style="width:42px;height:42px">{svg(i,22)}</div>' for i in ['heading','bold','checklist','quote','code','link'])
    body = f'''
<div class="rtop" style="background:var(--bg)"><div class="ibtn">{svg("close",24)}</div>
<div style="flex:1;margin-left:4px"><div style="font-size:16.5px;font-weight:700">编辑原文</div><div style="font-size:11.5px;color:#8E98AA">Markdown · 已修改</div></div>
<div class="ibtn">{svg("eye",22)}</div><div style="height:40px;border-radius:20px;background:#2F6BFF;color:#fff;font-weight:600;font-size:14.5px;display:flex;align-items:center;padding:0 20px;margin-left:4px">保存</div></div>
<div class="ed mono" style="font-size:14.5px">{md_src()}</div>
<div class="abs glass" style="left:12px;right:12px;bottom:276px;height:54px;border-radius:27px;z-index:30;display:flex;align-items:center;padding:0 8px;gap:0;justify-content:space-between">
<div class="ibtn" style="width:42px;color:#B3BBC9">{svg("undo",21)}</div><div class="ibtn" style="width:42px;color:#B3BBC9">{svg("redo",21)}</div><div style="width:1px;height:22px;background:#D5DBE6"></div>{tb}</div>
{kb()}'''
    return page(CSS+'.ed .l{white-space:pre-wrap}.l{min-height:25px}', body)

SCREENS = dict(home=home, reader=reader, share=share, typography=typo, more=more, edit=edit)
