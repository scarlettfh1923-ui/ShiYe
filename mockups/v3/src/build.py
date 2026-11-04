# -*- coding: utf-8 -*-
import os, subprocess, sys
sys.path.insert(0, os.path.dirname(__file__))
from data import *

OUT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

CSS = '''
:root{--bg:#F5F7FA;--acc:#2F6BFF;--acc-s:rgba(47,107,255,.09);--t1:#0F1522;--t2:#566074;--t3:#8A93A4;--hair:rgba(15,21,34,.09);
 --sh1:0 .5px 0 rgba(15,21,34,.04),0 1px 2px rgba(15,21,34,.04),0 6px 20px -4px rgba(15,21,34,.07)}
*{box-sizing:border-box;margin:0;padding:0}
html,body{width:360px;height:800px;overflow:hidden;background:var(--bg);color:var(--t1);-webkit-font-smoothing:antialiased;
 font-family:"Noto Sans CJK SC","PingFang SC",sans-serif;font-feature-settings:"palt" 0}
.serif{font-family:"Noto Serif CJK SC","Songti SC",serif}
.sq{corner-shape:squircle}
.screen{position:relative;width:360px;height:800px;overflow:hidden;background:var(--bg)}
svg.i{display:block;fill:none;stroke:currentColor;stroke-linecap:round;stroke-linejoin:round;flex:none}
.sb{position:absolute;top:0;left:0;right:0;height:44px;display:flex;justify-content:space-between;align-items:center;padding:8px 28px 0 32px;font-size:15px;font-weight:600;letter-spacing:-.2px;z-index:80;color:var(--t1)}
.sbr{display:flex;gap:6px;align-items:center}
.gest{position:absolute;bottom:8px;left:50%;width:120px;height:4px;margin-left:-60px;border-radius:2px;background:var(--t1);opacity:.85;z-index:90}
.abs{position:absolute}.row{display:flex;align-items:center}
/* top bar */
.topbar{position:absolute;top:52px;left:16px;right:16px;height:44px;display:flex;gap:12px}
.search{flex:1;height:44px;border-radius:22px;background:#fff;box-shadow:var(--sh1);display:flex;align-items:center;gap:8px;padding:0 16px;color:var(--t3);font-size:15px;letter-spacing:-.1px}
.plus{width:44px;height:44px;border-radius:22px;background:#fff;box-shadow:var(--sh1);display:flex;align-items:center;justify-content:center;color:var(--acc)}
/* sections */
.sec{display:flex;justify-content:space-between;align-items:baseline;padding:0 20px;margin:24px 0 12px}
.sec b{font-size:20px;line-height:28px;font-weight:700;letter-spacing:-.4px}.sec span{font-size:15px;color:var(--acc);font-weight:500}
/* hero (continue reading) */
.hero{margin:0 16px;background:#fff;border-radius:24px;padding:16px;display:flex;gap:16px;box-shadow:var(--sh1)}
.hero .tx{flex:1;min-width:0;display:flex;flex-direction:column;justify-content:space-between;padding:2px 0}
.eyebrow{font-size:12px;line-height:16px;color:var(--t3);font-weight:500;letter-spacing:.1px}
.hero h3{font-size:17px;line-height:24px;font-weight:600;letter-spacing:-.3px;margin-top:4px;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}
.pbar{height:3px;border-radius:2px;background:rgba(15,21,34,.07);overflow:hidden}.pbar i{display:block;height:100%;background:var(--acc);border-radius:2px}
.pmeta{font-size:12px;line-height:16px;color:var(--t2);margin-top:8px}
/* carousel */
.car{display:flex;gap:12px;padding:0 20px;overflow:hidden}
.car .c{width:112px;flex:none}
.c .cap{font-size:14px;line-height:20px;font-weight:600;letter-spacing:-.2px;margin-top:8px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;height:40px}
.c .mt{font-size:12px;line-height:16px;color:var(--t3);margin-top:4px}
/* mini list rows */
.lrow{display:flex;gap:16px;align-items:center;padding:8px 20px}
.lrow .tt{font-size:15px;line-height:22px;font-weight:600;letter-spacing:-.2px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.lrow .mt{font-size:12px;line-height:16px;color:var(--t3);margin-top:4px}
/* nav */
.nav{position:absolute;left:16px;right:16px;bottom:24px;height:64px;border-radius:32px;display:flex;align-items:center;padding:0 8px;z-index:30;
 background:rgba(255,255,255,.66);backdrop-filter:blur(28px) saturate(1.8);border:.5px solid rgba(255,255,255,.9);box-shadow:inset 0 1px 0 rgba(255,255,255,.9),0 .5px 0 rgba(15,21,34,.05),0 10px 32px -6px rgba(31,50,100,.2)}
.nav .n{flex:1;height:48px;border-radius:24px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;font-size:10px;font-weight:500;color:var(--t2)}
.nav .n.on{background:rgba(47,107,255,.11);color:var(--acc);font-weight:600}
/* covers: shared */
.cv{position:relative;overflow:hidden;flex:none;border-radius:var(--r,8px);corner-shape:squircle;
 box-shadow:0 .5px 0 rgba(15,21,34,.06),0 1px 2px rgba(15,21,34,.08),0 6px 14px -2px rgba(15,21,34,.12),0 16px 28px -10px rgba(15,21,34,.14)}
.cv:after{content:"";position:absolute;left:0;top:0;bottom:0;width:5px;background:linear-gradient(90deg,rgba(0,0,0,.09),rgba(255,255,255,.14) 55%,rgba(0,0,0,0));pointer-events:none}
.cv:before{content:"";position:absolute;inset:0;border-radius:inherit;box-shadow:inset 0 0 0 .5px rgba(15,21,34,.08);z-index:3;pointer-events:none;corner-shape:squircle}
/* opt1 typographic */
.t1c{display:flex;flex-direction:column;padding:var(--p) var(--p) var(--p) calc(var(--p) + 3px)}
.t1c .ey{font-size:var(--e);line-height:1.3;font-weight:600;letter-spacing:.08em;opacity:.78;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.t1c .ru{height:.5px;background:currentColor;opacity:.4;margin:calc(var(--p)*.55) 0 calc(var(--p)*.7);width:100%}
.t1c .ti{font-family:"Noto Serif CJK SC",serif;font-weight:700;font-size:var(--ft);line-height:var(--lh);letter-spacing:-.02em;display:-webkit-box;-webkit-line-clamp:var(--cl,4);-webkit-box-orient:vertical;overflow:hidden}
.t1c .su{font-size:var(--fs);line-height:var(--sl);font-weight:400;opacity:.72;margin-top:calc(var(--p)*.4);letter-spacing:0;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.t1c .ft{margin-top:auto;font-size:var(--e);opacity:.55;letter-spacing:.04em;display:flex;justify-content:space-between;align-items:center}
/* opt2 image */
.t2c{background-size:cover;background-position:center}
.t2c .sc{position:absolute;inset:0;background:linear-gradient(180deg,rgba(8,14,28,.18) 0%,rgba(8,14,28,0) 28%,rgba(8,14,28,.18) 52%,rgba(8,14,28,.82) 100%)}
.t2c .tx{position:absolute;left:var(--p);right:var(--p);bottom:var(--p);color:#fff}
.t2c .ey{font-size:var(--e);font-weight:600;letter-spacing:.08em;opacity:.78;margin-bottom:4px}
.t2c .ti{font-family:"Noto Serif CJK SC",serif;font-weight:700;font-size:var(--ft);line-height:var(--lh);letter-spacing:-.02em;text-shadow:0 1px 8px rgba(0,0,0,.28);display:-webkit-box;-webkit-line-clamp:4;-webkit-box-orient:vertical;overflow:hidden}
.t2c .su{font-size:var(--fs);line-height:var(--sl);opacity:.8;margin-top:2px}
/* opt3 pattern */
.t3c svg{position:absolute;inset:0}
.pill{position:absolute;left:8px;top:8px;height:16px;padding:0 6px;border-radius:8px;corner-shape:squircle;background:rgba(255,255,255,.72);backdrop-filter:blur(8px);font-size:8.5px;font-weight:700;letter-spacing:.04em;color:var(--acc);display:flex;align-items:center;z-index:4}
'''

def clock(): return '9:41'
SB = ('<div class="sb"><span>9:41</span><span class="sbr">'
 '<svg width="18" height="12" viewBox="0 0 18 12"><rect x="0" y="8" width="3" height="4" rx=".8" fill="currentColor"/><rect x="5" y="5.5" width="3" height="6.5" rx=".8" fill="currentColor"/><rect x="10" y="3" width="3" height="9" rx=".8" fill="currentColor"/><rect x="15" y="0" width="3" height="12" rx=".8" fill="currentColor"/></svg>'
 '<svg width="17" height="12" viewBox="0 0 17 12" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><path d="M1.5 4.2a9 9 0 0114 0M4 6.9a5.6 5.6 0 019 0"/><circle cx="8.5" cy="10" r="1.2" fill="currentColor" stroke="none"/></svg>'
 '<svg width="27" height="13" viewBox="0 0 27 13"><rect x=".5" y=".5" width="23" height="12" rx="3.8" fill="none" stroke="currentColor" opacity=".4"/><rect x="2" y="2" width="20" height="9" rx="2.4" fill="currentColor"/><rect x="25" y="4.5" width="1.6" height="4" rx=".8" fill="currentColor" opacity=".45"/></svg>'
 '</span></div><div class="gest"></div>')

def page(css, body, bg=None):
    st = f'<style>.screen,html,body{{background:{bg}}}</style>' if bg else ''
    return f'<!doctype html><html><head><meta charset="utf-8"><style>{CSS}{css}</style>{st}</head><body><div class="screen">{SB}{body}</div></body></html>'

def nav(active=0):
    it = [('home', 'home_f', '阅读'), ('library', 'lib_f', '书库'), ('user', 'user_f', '我的')]
    return '<div class="nav">' + ''.join(f'<div class="n {"on" if i==active else ""}">{ic(f if i==active else o, 22)}{l}</div>' for i, (o, f, l) in enumerate(it)) + '</div>'

def topbar(): return f'<div class="topbar"><div class="search sq">{ic("search",20)}在书库中搜索</div><div class="plus">{ic("plus",22,1.6)}</div></div>'

# ---------------- covers ----------------
T1 = [dict(bg='#FFFFFF', fg='#0F1522', ac='#2F6BFF'), dict(bg='#E8EFFF', fg='#142A5C', ac='#2F6BFF'), dict(bg='#2F6BFF', fg='#FFFFFF', ac='#FFFFFF'), dict(bg='#EDF0F5', fg='#1B2434', ac='#2F6BFF')]
SIZES = {   # w, h, p, eyebrow, title, lh, sub, sl, radius
 'card': (112, 150, 12, 8, 14, 19, 10.5, 15, 10), 'hero': (84, 112, 9, 6.5, 10, 14, 8, 11, 8), 'mini': (44, 58, 5, 0, 6.5, 8.6, 0, 0, 5)}

import re
def nw(s): return re.sub(r'(《[^》]*》)', r'<span style="white-space:nowrap">\1</span>', s)

def cover1(d, mode, v=0):
    w, h, p, e, ft, lh, fs, sl, r = SIZES[mode]; c = T1[v % 4]
    a, b = split_title(d['t'])
    ey = f'<div class="ey">{d["src"]}</div><div class="ru"></div>' if e else '<div style="height:.5px;background:currentColor;opacity:.4;margin:3px 0 4px"></div>'
    su = f'<div class="su">{b}</div>' if b and fs else ''
    ft_ = f'<div class="ft"><span>{d["date"] if d["date"] in ("今天","昨天") else d["date"]}</span><span style="width:{e*.9}px;height:{e*.9}px;border-radius:50%;background:{c["ac"]};opacity:.9"></span></div>' if e else ''
    return (f'<div class="cv t1c" style="width:{w}px;height:{h}px;--r:{r}px;--p:{p}px;--e:{e or 6}px;--ft:{ft}px;--lh:{lh}px;--fs:{fs}px;--sl:{sl}px;--cl:{4 if mode=="card" else 3};background:{c["bg"]};color:{c["fg"]}">'
            f'{ey}<div class="ti">{nw(a)}</div>{su}{ft_}</div>')

def cover2(d, mode, v=0):
    w, h, p, e, ft, lh, fs, sl, r = SIZES[mode]
    if not d['img']: return cover1(d, mode, 1 if v % 2 else 3)
    a, b = split_title(d['t'])
    tx = '' if mode == 'mini' else f'<div class="tx"><div class="ey">{d["src"]}</div><div class="ti">{nw(a)}</div>' + (f'<div class="su">{b}</div>' if b and fs else '') + '</div>'
    sc = '' if mode == 'mini' else '<div class="sc"></div>'
    return f'<div class="cv t2c" style="width:{w}px;height:{h}px;--r:{r}px;--p:{p}px;--e:{e}px;--ft:{ft}px;--lh:{lh}px;--fs:{fs}px;--sl:{sl}px;background-image:url({img_uri(d["img"])})">{sc}{tx}</div>'

def cover3(d, mode, v=0):
    w, h, p, e, ft, lh, fs, sl, r = SIZES[mode]
    pill = f'<div class="pill">{d["ty"]}</div>' if mode != 'mini' else ''
    return f'<div class="cv t3c" style="width:{w}px;height:{h}px;--r:{r}px">{pattern_svg(d["t"], w, h)}{pill}</div>'

# ---------------- home screens (opts 1-3) ----------------
def home123(opt):
    cov = {1: cover1, 2: cover2, 3: cover3}[opt]
    D = DOCS
    hero = D[0]
    car = [D[2], D[4], D[1], D[3]]
    if opt == 1: car = [D[2], D[1], D[3], D[4]]
    def card(d, i):
        title_below = (opt == 3)
        cap = f'<div class="cap">{d["t"]}</div>' if title_below else ''
        return f'<div class="c">{cov(d,"card",i)}{cap}<div class="mt">{d["src"]} · {d["date"]}</div></div>'
    cars = ''.join(card(d, i) for i, d in enumerate(car))
    def row(d, i):
        return f'<div class="lrow">{cov(d,"mini",i)}<div style="flex:1;min-width:0"><div class="tt">{d["t"]}</div><div class="mt">{d["src"]} · {d["date"]} · {d["mins"]} 分钟</div></div><div style="color:var(--acc)">{ic("bookmark_f",20)}</div></div>'
    hv = {1: 2, 2: 0, 3: 0}[opt]
    body = f'''{topbar()}
<div class="abs" style="top:96px;left:0;right:0">
<div class="sec" style="margin-top:20px"><b>继续阅读</b></div>
<div class="hero sq">{cov(hero,"hero",hv)}<div class="tx"><div><div class="eyebrow">{hero["plat"]} · {hero["src"]}</div><h3>{hero["t"]}</h3></div>
<div><div class="pbar"><i style="width:{hero["prog"]}%"></i></div><div class="pmeta">已读 {hero["prog"]}% · 还剩约 {round(hero["mins"]*(1-hero["prog"]/100))} 分钟</div></div></div></div>
<div class="sec"><b>最近添加</b><span>全部</span></div>
<div class="car">{cars}</div>
<div class="sec" style="margin-top:{20 if opt!=3 else 12}px"><b>收藏</b><span>全部</span></div>
{row(D[1],1)}{row(D[6],3)}{row(D[0],2)}
</div>{nav(0)}'''
    return page('', body)

# ---------------- option 4: Bear x Notes editorial list ----------------
CSS4 = '''
.chips{position:absolute;top:108px;left:0;right:0;display:flex;gap:8px;padding:0 16px;overflow:hidden}
.chip{height:32px;padding:0 14px;border-radius:16px;corner-shape:squircle;background:rgba(15,21,34,.05);font-size:14px;font-weight:500;color:var(--t2);display:flex;align-items:center;white-space:nowrap}
.chip.on{background:var(--acc);color:#fff;font-weight:600}
.chip.tg{background:#fff;box-shadow:0 0 0 .5px rgba(15,21,34,.1);color:var(--acc)}
.gh{font-size:13px;line-height:16px;font-weight:600;color:var(--t3);letter-spacing:.02em;padding:0 20px;margin:20px 0 8px}
.grp{margin:0 16px;background:#fff;border-radius:20px;corner-shape:squircle;box-shadow:var(--sh1);overflow:hidden}
.nr{position:relative;display:flex;gap:12px;padding:16px 16px 14px 16px}
.nr+.nr:before{content:"";position:absolute;top:0;left:16px;right:0;height:.5px;background:var(--hair)}
.nr .bd{flex:1;min-width:0}
.nr .tl{font-size:16px;line-height:22px;font-weight:600;letter-spacing:-.3px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.nr .ex{font-size:14px;line-height:20px;color:var(--t2);margin-top:4px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;letter-spacing:-.05px}
.nr .ft{display:flex;align-items:center;gap:8px;margin-top:8px;font-size:12px;line-height:16px;color:var(--t3);white-space:nowrap}
.tg{font-size:12px;color:var(--acc);font-weight:500;background:var(--acc-s);padding:0 8px;height:20px;border-radius:10px;corner-shape:squircle;display:inline-flex;align-items:center}
.nr .th{width:64px;height:64px;border-radius:12px;corner-shape:squircle;background-size:cover;background-position:center;flex:none;margin-top:2px;box-shadow:0 0 0 .5px rgba(15,21,34,.1),0 2px 6px -1px rgba(15,21,34,.14)}
.ring{width:14px;height:14px;border-radius:50%;flex:none}
'''
def ring(p): return f'<span class="ring" style="background:conic-gradient(var(--acc) {p*3.6}deg,rgba(15,21,34,.1) 0)"><span style="display:block;margin:2.5px;width:9px;height:9px;border-radius:50%;background:#fff"></span></span>'

def note_row(d):
    th = f'<div class="th" style="background-image:url({img_uri(d["img"])})"></div>' if d['img'] else ''
    tags = ''.join(f'<span class="tg">#{t}</span>' for t in d['tags'][:2])
    prog = f'{ring(d["prog"])}<span style="color:var(--t2)">已读 {d["prog"]}%</span>' if d['prog'] else f'<span>{d["time"]}</span>'
    pin = f'<span style="color:var(--acc);display:inline-flex">{ic("pin",14,1.5)}</span>' if d['fav'] else ''
    return f'<div class="nr"><div class="bd"><div class="tl">{d["t"]}</div><div class="ex">{d["ex"]}</div><div class="ft">{pin}{prog}<span style="width:2px"></span>{tags}</div></div>{th}</div>'

def home4():
    D = DOCS
    chips = '<div class="chip on">全部</div><div class="chip">收藏</div><div class="chip tg">#电影</div><div class="chip tg">#读书</div><div class="chip tg">#设计</div><div class="chip tg">#Android</div>'
    body = f'''{topbar()}<div class="chips">{chips}</div>
<div class="abs" style="top:148px;left:0;right:0">
<div class="gh">今天</div><div class="grp">{note_row(D[0])}{note_row(D[1])}</div>
<div class="gh">昨天</div><div class="grp">{note_row(D[2])}{note_row(D[3])}</div>
</div>{nav(0)}'''
    return page(CSS4, body)

# ---------------- reader header ----------------
CSSR = '''
.page{position:absolute;inset:0;background:#FAFBFD}
.rd{position:absolute;top:60px;left:0;right:0;padding:0 28px}
.rd .ey{display:flex;align-items:center;gap:8px;font-size:12px;line-height:16px;font-weight:600;color:var(--acc);letter-spacing:.06em}
.rd .ey i{width:16px;height:16px;border-radius:5px;corner-shape:squircle;background:var(--acc);display:block}
.rd .ey span{color:var(--t3);font-weight:500;letter-spacing:.02em}
.rd h1{font-family:"Noto Serif CJK SC",serif;font-weight:700;font-size:30px;line-height:42px;letter-spacing:-.025em;margin-top:16px;text-wrap:balance}
.rd h2.sub{font-family:"Noto Serif CJK SC",serif;font-weight:400;font-size:20px;line-height:28px;letter-spacing:-.01em;color:var(--t2);margin-top:4px}
.rd .dek{font-size:15px;line-height:24px;color:var(--t2);margin-top:16px;letter-spacing:-.05px}
.rd .tags{display:flex;gap:8px;margin-top:16px}
.rd .meta{display:flex;align-items:center;margin-top:20px;padding:12px 0;border-top:.5px solid var(--hair);border-bottom:.5px solid var(--hair);font-size:12px;line-height:16px;color:var(--t2)}
.rd .meta div{flex:1;display:flex;align-items:center;gap:8px}.rd .meta div+div{padding-left:16px;border-left:.5px solid var(--hair)}
.rd .meta b{color:var(--t1);font-weight:600}
.rd .body h3+p{margin-top:8px}
.rd .body{margin-top:24px;font-family:"Noto Serif CJK SC",serif;font-size:17px;line-height:30px;color:#1A2231;text-align:justify;text-justify:inter-character;letter-spacing:.01em}
.rd .body p+p,.rd .body h3{margin-top:12px}
.rd .body h3{font-family:"Noto Sans CJK SC",sans-serif;font-size:20px;line-height:28px;font-weight:700;letter-spacing:-.3px;margin-top:24px;padding-top:4px}
.tg{font-size:12px;color:var(--acc);font-weight:500;background:var(--acc-s);padding:0 8px;height:20px;border-radius:10px;corner-shape:squircle;display:inline-flex;align-items:center}
.foot{position:absolute;left:28px;right:28px;bottom:28px;font-size:11px;color:var(--t3);display:flex;justify-content:space-between}
.hair{position:absolute;left:0;bottom:0;height:2px;width:6%;background:var(--acc)}
'''
def reader():
    d = DOCS[0]; a, b = split_title(d['t'])
    tags = ''.join(f'<span class="tg">#{t}</span>' for t in d['tags'])
    body = f'''<div class="page"></div>
<div class="rd"><div class="ey"><i></i>{d["src"].upper()}<span>·  {d["plat"]}  ·  10月31日</span></div>
<h1>{a}</h1><h2 class="sub">{b}</h2>
<div class="dek">{d["ex"]}</div><div class="tags">{tags}</div>
<div class="meta"><div>{ic("clock",16)}<span>约 <b>{d["mins"]}</b> 分钟</span></div><div>{ic("file",16)}<span><b>{d["words"]}</b> 字</span></div><div>{ic("toc",16)}<span><b>5</b> 节</span></div></div>
<div class="body"><p>1947 年，文华影片公司出品《太太万岁》，张爱玲编剧，桑弧导演。一个操持全家、八面玲珑的太太，在一间弄堂房子里，把日子过成了喜剧。</p><h3>一、弄堂里的喜剧</h3><p>七年后，同一位导演与黄沙合作，把越剧《梁山伯与祝英台》搬上银幕，第一次让戏曲在胶片上有了颜色。</p></div></div>
<div class="foot"><span>{a[:12]}…</span><span>0% · 还剩 9 分钟</span></div><div class="hair"></div>'''
    return page(CSSR, body, '#FAFBFD')

SCREENS = {'option1_home': lambda: home123(1), 'option2_home': lambda: home123(2), 'option3_home': lambda: home123(3), 'option4_home': home4, 'reader_header': reader}

def shot(name, html):
    hp = f'{OUT}/html/{name}.html'; open(hp, 'w', encoding='utf-8').write(html)
    pp = f'{OUT}/{name}.png'
    subprocess.run(['google-chrome', '--headless=new', '--no-sandbox', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=3', '--window-size=360,800', '--virtual-time-budget=2000', f'--screenshot={pp}', 'file://' + hp], stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL)
    print(pp)

if __name__ == '__main__':
    for n, fn in SCREENS.items():
        if len(sys.argv) > 1 and n not in sys.argv[1:]: continue
        shot(n, fn())
