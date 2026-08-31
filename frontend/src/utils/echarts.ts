import * as echarts from 'echarts/core'
import { LineChart, BarChart, GraphChart, PieChart, GaugeChart } from 'echarts/charts'
import { GridComponent, TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  BarChart,
  GraphChart,
  PieChart,
  GaugeChart,
  GridComponent,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  CanvasRenderer,
])

export * from 'echarts/core'