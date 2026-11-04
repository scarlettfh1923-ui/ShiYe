# -*- coding: utf-8 -*-
import base64, random, re, sys, os
sys.path.insert(0, '/workspace/apk-project/ShiYe/mockups/v2/src')
from common import ICONS

def ic(name, s=24, sw=1.5, cls=''):
    return f'<svg class="i {cls}" width="{s}" height="{s}" viewBox="0 0 24 24" style="stroke-width:{sw}">{ICONS[name]}</svg>'

DOCS = [
 dict(t="在《太太万岁》与《梁祝》之间——桑弧的两个上海", src="夜航影话", plat="微信公众号", date="今天", time="09:12", mins=9, words="4,126", tags=["电影","上海"], img="sh", prog=18, fav=True, ty="MD",
      ex="一部是 1947 年张爱玲编剧的都市喜剧，一部是 1954 年的越剧彩色影片。桑弧用两部几乎没有交集的电影，拍下了上海在不同年代的两种神情。"),
 dict(t="周末读书笔记：慢下来的力量", src="微信", plat="微信", date="今天", time="08:40", mins=4, words="1,820", tags=["读书"], img=None, prog=42, fav=True, ty="MD",
      ex="真正的效率，来自于对注意力的尊重。每天固定 25 分钟阅读，不看手机，读完写三句话总结，周末再回头挑一条付诸行动。"),
 dict(t="京都五日：被雨水洗过的街道", src="备忘录", plat="备忘录", date="昨天", time="21:05", mins=6, words="2,640", tags=["旅行","日本"], img="kyoto", prog=0, fav=False, ty="MD",
      ex="11 月初出发：伏见稻荷、岚山、哲学之道，雨天备选方案写在每天的末尾。比起景点，更想记下巷口那家只开到傍晚的茶屋。"),
 dict(t="Kotlin 协程取消机制的五个坑", src="GitHub", plat="GitHub", date="昨天", time="18:32", mins=7, words="3,051", tags=["Android"], img=None, prog=0, fav=False, ty="HTML",
      ex="cancel 不是立即停止，而是协作式的。从 try/finally 里的挂起函数，到被吞掉的 CancellationException，逐个复现并给出写法。"),
 dict(t="设计系统里的间距：从 4pt 到 8pt", src="Safari", plat="Safari", date="周三", time="10:20", mins=5, words="2,208", tags=["设计"], img="grid", prog=0, fav=False, ty="HTML",
      ex="为什么大多数团队最后都落在 8pt 网格上？它如何帮助我们在不同密度的屏幕上保持节奏，又在哪些时候应该打破它。"),
 dict(t="为什么我们总是读不完一本书", src="夜航影话", plat="微信公众号", date="周二", time="22:14", mins=8, words="3,540", tags=["读书"], img=None, prog=0, fav=False, ty="HTML",
      ex="收藏夹里躺着几百篇「以后再读」，真正的问题不是时间，而是入口太多、出口太少。"),
 dict(t="Q4 路线图评审会议纪要", src="粘贴", plat="粘贴", date="周一", time="16:48", mins=3, words="1,206", tags=["工作"], img=None, prog=0, fav=True, ty="MD",
      ex="结论先行：首页改版排在 11 月第一周，导出能力放到第二周，Notion 同步随下一版上线。"),
]

def split_title(t):
    for sep in ("——", "：", ":"):
        if sep in t:
            a, b = t.split(sep, 1); return a.strip(), b.strip()
    return t, ""

# ---------- procedural "photos" (SVG) ----------
def _grain(i):
    return f'<filter id="g{i}"><feTurbulence type="fractalNoise" baseFrequency=".85" numOctaves="2" seed="{i}"/><feColorMatrix values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 .55 0"/></filter>'

def scene(key):
    rnd = random.Random(key)
    if key == 'sh':   # 老上海：黄昏、薄雾、亮窗的楼群
        s = ['<defs><linearGradient id="a" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#16314f"/><stop offset=".45" stop-color="#6f93a8"/><stop offset=".78" stop-color="#e9c79b"/><stop offset="1" stop-color="#f4dcb8"/></linearGradient>'
             '<filter id="b"><feGaussianBlur stdDeviation="2.4"/></filter><filter id="b2"><feGaussianBlur stdDeviation="7"/></filter>'
             '<radialGradient id="sun" cx=".7" cy=".62" r=".5"><stop offset="0" stop-color="#fff3d6" stop-opacity=".95"/><stop offset="1" stop-color="#fff3d6" stop-opacity="0"/></radialGradient>'+_grain(3)+'</defs>',
             '<rect width="300" height="400" fill="url(#a)"/><rect width="300" height="400" fill="url(#sun)"/>']
        x = -10
        while x < 310:   # 远景
            w = rnd.randint(22, 40); h = rnd.randint(70, 150)
            s.append(f'<rect x="{x}" y="{300-h}" width="{w}" height="{h+120}" fill="#8aa5b4" opacity=".55" filter="url(#b)"/>'); x += w - 4
        s.append('<g filter="url(#b2)"><rect x="0" y="250" width="300" height="60" fill="#f1dcc0" opacity=".55"/></g>')
        x = -14
        while x < 310:   # 中景
            w = rnd.randint(30, 56); h = rnd.randint(110, 220)
            s.append(f'<rect x="{x}" y="{330-h}" width="{w}" height="{h+80}" fill="#26384c"/>')
            for wy in range(330-h+8, 330, 11):
                for wx in range(x+5, x+w-6, 9):
                    if rnd.random() < .34: s.append(f'<rect x="{wx}" y="{wy}" width="4" height="6" rx=".6" fill="#ffd28a" opacity="{rnd.uniform(.5,.95):.2f}"/>')
            x += w + rnd.randint(-2, 6)
        s.append('<path d="M214 70 l10 -26 l10 26z" fill="#1a2a3d"/><rect x="209" y="70" width="30" height="140" fill="#1a2a3d"/><rect x="216" y="80" width="16" height="3" fill="#ffd28a" opacity=".8"/>')   # 钟楼
        s.append('<rect x="0" y="318" width="300" height="82" fill="#101a28"/>')
        for i in range(7): s.append(f'<ellipse cx="{30+i*42}" cy="{338+(i%2)*10}" rx="{10+i%3*4}" ry="2" fill="#ffcf85" opacity=".5"/>')
        s.append('<rect width="300" height="400" filter="url(#g3)" opacity=".16" style="mix-blend-mode:overlay"/>')
    elif key == 'kyoto':   # 京都：雨夜灯笼与湿润路面
        s = ['<defs><linearGradient id="a" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0d1a30"/><stop offset=".5" stop-color="#21405f"/><stop offset=".62" stop-color="#c98a58"/><stop offset="1" stop-color="#1a1d29"/></linearGradient>'
             '<filter id="b"><feGaussianBlur stdDeviation="5"/></filter><filter id="b1"><feGaussianBlur stdDeviation="1.4"/></filter>'
             '<radialGradient id="l"><stop offset="0" stop-color="#ffd9a0"/><stop offset=".35" stop-color="#ff9a4a" stop-opacity=".9"/><stop offset="1" stop-color="#ff7a2a" stop-opacity="0"/></radialGradient>'+_grain(5)+'</defs>',
             '<rect width="300" height="400" fill="url(#a)"/>']
        s.append('<path d="M0 0 H300 V96 L240 70 L150 104 L60 66 L0 92Z" fill="#0a1220"/>')
        s.append('<path d="M0 92 L60 66 L150 104 L240 70 L300 96 V108 L240 84 L150 118 L60 80 L0 106Z" fill="#1a2740"/>')
        s.append('<rect x="0" y="232" width="300" height="168" fill="#14161f"/>')
        for i,(cx,cy,r) in enumerate([(52,150,26),(112,172,18),(190,160,22),(250,188,16),(150,214,10),(24,204,13)]):
            s.append(f'<circle cx="{cx}" cy="{cy}" r="{r*2.2}" fill="url(#l)" opacity=".85" filter="url(#b)"/><ellipse cx="{cx}" cy="{cy}" rx="{r*.42}" ry="{r*.62}" fill="#ffe3b5" opacity=".95"/>')
            s.append(f'<rect x="{cx-1}" y="{cy+r*.62}" width="2" height="{rnd.randint(10,26)}" fill="#0c0f18" opacity=".6"/>')
            s.append(f'<rect x="{cx-r*.5}" y="236" width="{r}" height="{rnd.randint(70,150)}" fill="url(#l)" opacity=".45" filter="url(#b)"/>')
        for i in range(60):
            x = rnd.randint(0, 300); y = rnd.randint(0, 400)
            s.append(f'<rect x="{x}" y="{y}" width=".8" height="{rnd.randint(8,18)}" fill="#cfe0ff" opacity=".25"/>')
        s.append('<rect width="300" height="400" filter="url(#g5)" opacity=".14" style="mix-blend-mode:overlay"/>')
    else:   # grid：8pt 网格上的卡片静物
        s = ['<defs><linearGradient id="a" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#F3F7FF"/><stop offset="1" stop-color="#D6E2FA"/></linearGradient>'
             '<filter id="sh" x="-30%" y="-30%" width="160%" height="170%"><feDropShadow dx="0" dy="10" stdDeviation="9" flood-color="#1b3a8a" flood-opacity=".22"/></filter></defs>',
             '<rect width="300" height="400" fill="url(#a)"/>']
        for i in range(0, 300, 20): s.append(f'<path d="M{i} 0V400" stroke="#2F6BFF" stroke-opacity=".08"/>')
        for j in range(0, 400, 20): s.append(f'<path d="M0 {j}H300" stroke="#2F6BFF" stroke-opacity=".08"/>')
        s.append('<g filter="url(#sh)"><rect x="40" y="70" width="150" height="100" rx="14" fill="#fff" transform="rotate(-6 115 120)"/>'
                 '<rect x="96" y="150" width="160" height="110" rx="14" fill="#2F6BFF" transform="rotate(5 176 205)"/>'
                 '<rect x="52" y="244" width="120" height="84" rx="14" fill="#EAF0FF" transform="rotate(-3 112 286)"/></g>')
        s.append('<g transform="rotate(-6 115 120)" fill="#2F6BFF"><rect x="56" y="86" width="60" height="8" rx="4"/><rect x="56" y="104" width="100" height="5" rx="2.5" opacity=".25"/><rect x="56" y="116" width="84" height="5" rx="2.5" opacity=".25"/></g>')
        s.append('<g transform="rotate(5 176 205)" fill="#fff"><rect x="114" y="168" width="68" height="8" rx="4"/><rect x="114" y="186" width="110" height="5" rx="2.5" opacity=".5"/></g>')
        s.append('<path d="M28 360h244" stroke="#2F6BFF" stroke-opacity=".5"/><path d="M28 354v12M68 356v8M108 356v8M148 354v12M188 356v8M228 356v8M268 354v12" stroke="#2F6BFF" stroke-opacity=".5"/>')
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 400" preserveAspectRatio="xMidYMid slice">{"".join(s)}</svg>'

def img_uri(key):
    return 'data:image/svg+xml;base64,' + base64.b64encode(scene(key).encode()).decode()

# ---------- seeded generative pattern ----------
def fnv(s):
    h = 2166136261
    for ch in s.encode('utf-8'): h = ((h ^ ch) * 16777619) & 0xFFFFFFFF
    return h

BLUES = ['#2F6BFF', '#6C93FF', '#9DB8FF', '#BFD1FF', '#5B8CFF', '#8CA9FF', '#7FB0FF']
def pattern_svg(title, W=120, H=160):
    h = fnv(title); r = random.Random(h); kind = h % 4
    pid = f'p{h}'
    o = [f'<defs><linearGradient id="{pid}b" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#F7F9FF"/><stop offset="1" stop-color="#E4ECFF"/></linearGradient><filter id="{pid}f" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="{W*.16:.1f}"/></filter></defs>',
         f'<rect width="{W}" height="{H}" fill="url(#{pid}b)"/><g filter="url(#{pid}f)">']
    for i in range(4):
        cx = r.uniform(-.1, 1.1) * W; cy = r.uniform(-.1, 1.1) * H; rr = r.uniform(.28, .5) * H
        o.append(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{rr:.1f}" fill="{r.choice(BLUES)}" opacity="{r.uniform(.4,.75):.2f}"/>')
    o.append('</g><g fill="none" stroke="#2F6BFF" stroke-width=".55" stroke-linecap="round">')
    if kind == 0:   # 同心圆
        cx = r.uniform(.2, .8) * W; cy = r.uniform(.3, .9) * H
        for k in range(1, 15): o.append(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{k*5.2:.1f}" opacity="{.30-k*.015:.2f}"/>')
    elif kind == 1:   # 莫尔条纹
        a = r.uniform(-30, 30)
        for t in (0, 2.4):
            o.append(f'<g transform="rotate({a+t:.1f} {W/2} {H/2})" opacity=".2">' + ''.join(f'<path d="M-40 {y}H{W+40}"/>' for y in range(-20, H+40, 4)) + '</g>')
    elif kind == 2:   # 扇形弧
        ox = r.choice([0, W]); oy = r.choice([0, H])
        for k in range(1, 18): o.append(f'<circle cx="{ox}" cy="{oy}" r="{k*8}" opacity="{.28-k*.012:.2f}"/>')
    else:   # 细线网格 + 斜切
        s = r.choice([12, 16]); rot = r.uniform(-18, 18)
        o.append(f'<g transform="rotate({rot:.1f} {W/2} {H/2})" opacity=".22">' + ''.join(f'<path d="M{x} -40V{H+40}"/>' for x in range(-40, W+40, s)) + ''.join(f'<path d="M-40 {y}H{W+40}"/>' for y in range(-40, H+40, s)) + '</g>')
    o.append('</g>')
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" preserveAspectRatio="xMidYMid slice" width="100%" height="100%">{"".join(o)}</svg>'
