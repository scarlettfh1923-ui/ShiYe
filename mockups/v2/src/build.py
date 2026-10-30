import sys, importlib, os, subprocess
sys.path.insert(0, os.path.dirname(__file__))
OUT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
targets = sys.argv[1:] or ['a','b','c']
for d in targets:
    m = importlib.import_module('dir_'+d)
    os.makedirs(f'{OUT}/html', exist_ok=True)
    for name, fn in m.SCREENS.items():
        hp = f'{OUT}/html/{d}_{name}.html'
        open(hp,'w',encoding='utf-8').write(fn())
        pp = f'{OUT}/direction_{d.upper()}_{name}.png'
        subprocess.run(['google-chrome','--headless=new','--no-sandbox','--disable-gpu','--hide-scrollbars','--force-device-scale-factor=3','--window-size=360,800','--virtual-time-budget=1500',f'--screenshot={pp}','file://'+hp],stderr=subprocess.DEVNULL,stdout=subprocess.DEVNULL)
        print(pp)
