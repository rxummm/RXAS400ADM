import DefaultTheme from 'vitepress/theme'
import { onMounted } from 'vue'
import mermaid from 'mermaid'

export default {
  extends: DefaultTheme,
  setup() {
    onMounted(() => {
      mermaid.initialize({
        startOnLoad: true,
        securityLevel: 'loose',
        theme: 'default',
      })
      mermaid.run()
    })
  },
}