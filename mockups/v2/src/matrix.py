# -*- coding: utf-8 -*-
import subprocess, os
OUT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
rows=[('功能','v1.1（现状）','A · M3 Expressive','B · 沉浸阅读','C · 卡片库'),
('阅读','首页列表点进','列表 → 阅读页','「继续阅读」大卡 / 横滑封面','瀑布流卡片（含摘要）'),
('搜索','首页普通输入框','首页搜索胶囊（一级）','首页搜索胶囊 + 头像','☰ 搜索栏 + chip 筛选'),
('导入 / 新建','首页多个入口','扩展 FAB（一级）→ 粘贴/文件/剪藏','FAB + 号（一级）','底部操作条 4 键 + FAB（一级）'),
('目录','阅读页一级','悬浮工具栏（一级）','底部工具条（点屏唤出）','底部胶囊（一级）'),
('排版','阅读页一级（挡内容）','悬浮工具栏 → 非模态面板（二级）','工具条 → 面板：主题/字体/步进器（二级）','胶囊 Aa → 锚点气泡（二级）'),
('编辑','阅读页一级','悬浮工具栏 → 编辑页（二级）','工具条 → 编辑页（二级）','胶囊 → 编辑页（二级）'),
('收藏','⋯ 内/顶栏','阅读页顶栏星标 + 底部导航「收藏」','顶栏书签 + 首页「收藏」分区','顶栏书签 + chip「收藏」'),
('分享：小红书 / 长图 / Notion / 文件','底部分享条（一排挤满）','分享 FAB → 2×2 磁贴（二级）','分享 → 分段发布面板，带实时预览（二级）','分享 → 缩略图列表 → 独立导出页（三级）'),
('设置','首页入口','「我的」Tab','「我的」Tab','侧边抽屉'),
('低频：重命名/信息/历史/删除','混杂在 ⋯ 与分享条','⋯ 浮层菜单（三级）','⋯ 浮层菜单（三级）','⋯ 底部菜单（带编辑时间元信息）')]
CSS='''*{margin:0;box-sizing:border-box}body{width:2000px;padding:50px 56px;background:#F5F7FA;font-family:"Noto Sans CJK SC";color:#141B2B}h1{font-size:40px;margin-bottom:8px}.s{font-size:20px;color:#58637A;margin-bottom:28px}
table{border-collapse:separate;border-spacing:0;width:100%;background:#fff;border-radius:22px;overflow:hidden;box-shadow:0 8px 28px rgba(30,50,100,.1)}th{background:#2F6BFF;color:#fff;font-size:19px;padding:16px 18px;text-align:left}td{padding:15px 18px;font-size:17.5px;line-height:25px;border-top:1px solid #E6EAF2;color:#2A3446;vertical-align:top}td:first-child{font-weight:700;background:#F0F4FB;width:220px}td:nth-child(2){color:#8E98AA}'''
h='<!doctype html><meta charset="utf-8"><style>'+CSS+'</style><h1>拾页 v2 · 功能放在哪一层？</h1><div class="s">一级 = 常驻可见；二级 = 点一下展开；三级 = 进入独立页或低频菜单。四种方案逐项对照。</div><table>'
for i,r in enumerate(rows):
    t='th' if i==0 else 'td'
    h+='<tr>'+''.join(f'<{t}>{c}</{t}>' for c in r)+'</tr>'
h+='</table>'
p=f'{OUT}/_m.html';open(p,'w',encoding='utf-8').write(h)
subprocess.run(['google-chrome','--headless=new','--no-sandbox','--disable-gpu','--hide-scrollbars','--window-size=2112,1000',f'--screenshot={OUT}/00_overview_matrix.png','file://'+p],stderr=subprocess.DEVNULL,stdout=subprocess.DEVNULL)
os.remove(p)
