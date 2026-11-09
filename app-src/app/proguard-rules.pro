# 拾页：不使用反射、序列化库，无需额外保留规则。
# 2.2.0 新增 OkHttp 4.12（自带 consumer rules，已含 conscrypt/bouncycastle/openjsse 的 dontwarn）与 jsoup 1.18（不靠反射）：R8 构建无缺类告警，不需另加规则。
# 数据用 org.json 手写读写；WebView 不开 JS 接口。
