# -*- coding: utf-8 -*-
import subprocess, os
OUT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
D = {
'A': dict(name='方向 A · Material 3 Expressive 标准派', tag='「最像 Google 官方」：搜索胶囊 + 分组列表 + 悬浮工具栏 + 弹性动效。工程风险最低，最稳。',
  tiers=[('一级（常驻）','底部导航 4 项：阅读 · 收藏 · 导出历史 · 我的；首页搜索胶囊；扩展 FAB「新建/导入」；阅读页悬浮工具栏：目录 / 排版 / 编辑 + 分享 FAB'),
         ('二级（一次点击）','排版 → 非模态底部面板（滑块实时预览）；分享 → 2×2 磁贴面板（小红书 / 长图 / Notion / 文件）；⋯ → 浮层菜单'),
         ('三级（低频）','重命名 · 文档信息 · 历史版本 · 在 Notion 打开 · 删除；设置在「我的」')]),
'B': dict(name='方向 B · Play 图书式沉浸阅读', tag='「读的时候什么都不挡」：阅读页默认全屏无 chrome，点一下出玻璃工具；导出集中在一张带预览的发布面板。',
  tiers=[('一级（常驻）','底部 3 项：阅读 · 书库 · 我的；首页=「继续阅读」大卡 + 最近添加横滑 + 收藏；顶部搜索胶囊含头像'),
         ('二级（点屏幕唤出）','底部玻璃工具条：进度条（带章节刻度）+ 目录 / 排版 / 编辑 / 分享；选中文字 → 浮条（复制 · 高亮 · 做成金句卡）'),
         ('三级','排版面板 = 主题 + 字体 + 三个步进器；分享面板 = 分段（小红书/长图/Notion/文件）+ 实时预览 + 样式 + 比例；⋯ 菜单只放低频项')]),
'C': dict(name='方向 C · Keep 式卡片库 + 轻玻璃', tag='「库是主角」：瀑布流卡片带正文摘要，标签 chip 筛选，侧边抽屉管合集；阅读页只留一个悬浮玻璃胶囊。',
  tiers=[('一级（常驻）','无底部导航：顶部搜索胶囊（☰ 抽屉 / 视图切换 / 头像）+ 筛选 chip + 卡片瀑布；底部操作条：粘贴 · 链接剪藏 · 文件 · 新建；FAB'),
         ('二级（点击）','阅读页：顶部玻璃条（返回 / 搜索 / 收藏 / ⋯）+ 底部悬浮胶囊（目录 · Aa · 编辑 · 分享）；Aa → 锚点气泡浮层'),
         ('三级（独立页）','分享 = 缩略图列表，进入后各有独立导出页（样式 / 比例 / 预览）；⋯ = 带「编辑于…」元信息的底部菜单；设置/合集在抽屉')]),
}
ORDER=[('home','首页'),('reader','阅读'),('share','分享 / 导出'),('typography','排版'),('more','⋯ 更多'),('edit','编辑原文')]
CSS='''*{margin:0;box-sizing:border-box}body{width:2060px;background:linear-gradient(180deg,#EEF2F9,#E4EAF5);font-family:"Noto Sans CJK SC",sans-serif;color:#141B2B;padding:56px 60px 60px}
h1{font-size:44px;font-weight:800}.tag{font-size:22px;color:#4A566D;margin:10px 0 36px;max-width:1700px;line-height:32px}
.row{display:flex;gap:28px}.ph{width:300px}.ph .f{border-radius:34px;overflow:hidden;box-shadow:0 18px 40px rgba(30,50,100,.22),0 0 0 6px #fff;background:#fff}
.ph img{display:block;width:300px;height:667px}.ph .c{text-align:center;font-size:20px;font-weight:700;margin-top:26px;color:#2A3446}
.ia{display:flex;gap:22px;margin-top:34px}.ia div{flex:1;background:rgba(255,255,255,.8);border-radius:22px;padding:20px 24px}
.ia b{display:block;font-size:19px;color:#2F6BFF;margin-bottom:8px}.ia span{font-size:17px;line-height:27px;color:#2A3446}
.ft{margin-top:18px;font-size:15px;color:#7A869D}'''
for k,v in D.items():
    ph=''.join(f'<div class="ph"><div class="f"><img src="direction_{k}_{n}.png"></div><div class="c">{l}</div></div>' for n,l in ORDER)
    ia=''.join(f'<div><b>{a}</b><span>{b}</span></div>' for a,b in v['tiers'])
    html=f'<!doctype html><meta charset="utf-8"><style>{CSS}</style><h1>{v["name"]}</h1><div class="tag">{v["tag"]}</div><div class="row">{ph}</div><div class="ia">{ia}</div><div class="ft">拾页 ShiYe v2 方案稿 · 冷白 #F5F7FA · 单一强调色 #2F6BFF · 细线图标 · 轻玻璃（仅静态示意，未改任何代码）</div>'
    hp=f'{OUT}/html/compare_{k}.html'; open(hp,'w',encoding='utf-8').write(html)
    # place html next to pngs for relative imgs
    hp2=f'{OUT}/_cmp_{k}.html'; open(hp2,'w',encoding='utf-8').write(html)
    subprocess.run(['google-chrome','--headless=new','--no-sandbox','--disable-gpu','--hide-scrollbars','--force-device-scale-factor=1','--window-size=2180,1135','--virtual-time-budget=2500',f'--screenshot={OUT}/direction_{k}_compare.png','file://'+hp2],stderr=subprocess.DEVNULL,stdout=subprocess.DEVNULL)
    os.remove(hp2)
