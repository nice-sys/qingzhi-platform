import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import pinia from './stores'
import { installPermissionDirective } from './directives/permission'
import './styles/index.css'

const app = createApp(App)

/* Element Plus 图标全量注册（<el-icon><component :is="'HomeFilled'" /></el-icon>） */
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, {
  locale: zhCn,
  size: 'default'
})
app.use(pinia)
app.use(router)
installPermissionDirective(app)

app.mount('#app')
