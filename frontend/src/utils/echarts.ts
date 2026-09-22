import * as echarts from 'echarts/core'
import { LineChart, BarChart, GraphChart, PieChart, GaugeChart, HeatmapChart } from 'echarts/charts'
import { GridComponent, TitleComponent, TooltipComponent, LegendComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  BarChart,
  GraphChart,
  PieChart,
  GaugeChart,
  HeatmapChart,
  GridComponent,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  VisualMapComponent,
  CanvasRenderer,
])

export * from 'echarts/core'