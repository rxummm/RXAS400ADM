/**
 * generate-flow-diagrams.cjs
 * 
 * 扫描后端 Java 源码，从 Controller/Service/Mapper 注解自动生成 Mermaid 时序图。
 * 
 * 用法：
 *   node docs/scripts/generate-flow-diagrams.cjs
 * 
 * 输出：
 *   docs/flowcharts/generated/ 目录下按模块生成 .md 文件（含 Mermaid 时序图）
 * 
 * 扫描策略：
 *   - Controller: 提取 @GetMapping/@PostMapping/@PutMapping/@DeleteMapping 路径
 *   - Service: 提取 @Autowired 注入的 Mapper 依赖
 *   - Mapper: 提取 @TableName 注解获取表名、方法名推断 SQL 操作
 *   - 权限: 提取 @PreAuthorize 注解中的权限码
 *   - 操作日志: 提取 @OperateLog 注解中的描述
 * 
 * 注意：此脚本为辅助工具，建议定期运行以保持文档与代码同步。
 *       手写流程图（01-auth.md ~ 10-tables.md）不会被覆盖。
 */

const fs = require('fs');
const path = require('path');

// ============ 配置 ============
const BACKEND_ROOT = path.resolve(__dirname, '..', '..', 'backend');
const OUTPUT_DIR = path.resolve(__dirname, '..', 'flowcharts', 'generated');
const MODULES = [
  'rxas400adm-app',
  'rxas400adm-as400',
  'rxas400adm-system',
  'rxas400adm-security',
  'rxas400adm-email',
  'rxas400adm-report',
  'rxas400adm-monitor',
];

// ============ 数据模型 ============
class Endpoint {
  constructor() {
    this.httpMethod = '';       // GET/POST/PUT/DELETE
    this.apiPath = '';          // /api/v1/users
    this.methodName = '';       // listUsers
    this.permission = '';       // 权限码
    this.operateLog = '';       // 操作日志描述
    this.controller = '';       // 类名
    this.services = [];         // 注入的 Service
    this.serviceCalls = [];     // 调用的 Service 方法
    this.mappers = [];          // 涉及的 Mapper
    this.tables = [];           // 涉及的数据表
  }
}

// ============ 扫描函数 ============

function findAllJavaFiles(dir) {
  const results = [];
  function walk(d) {
    if (!fs.existsSync(d)) return;
    const entries = fs.readdirSync(d, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(d, entry.name);
      if (entry.isDirectory() && !entry.name.startsWith('.') && entry.name !== 'target') {
        walk(fullPath);
      } else if (entry.isFile() && entry.name.endsWith('.java')) {
        results.push(fullPath);
      }
    }
  }
  walk(dir);
  return results;
}

function readFileSafe(filePath) {
  try {
    return fs.readFileSync(filePath, 'utf8');
  } catch {
    return '';
  }
}

function extractAnnotations(source) {
  const result = {
    requestMappings: [],
    autowired: [],
    preAuthorize: '',
    operateLog: '',
    tableName: '',
    className: '',
    packageName: '',
  };

  result.className = (source.match(/public\s+class\s+(\w+)/) || [])[1] || '';
  result.packageName = (source.match(/package\s+([\w.]+);/) || [])[1] || '';

  // @GetMapping / @PostMapping / @PutMapping / @DeleteMapping
  const mappingRegex = /@(Get|Post|Put|Delete)Mapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/g;
  let match;
  while ((match = mappingRegex.exec(source)) !== null) {
    result.requestMappings.push({
      method: match[1].toUpperCase(),
      path: match[2],
    });
  }

  // @RequestMapping
  const reqMapRegex = /@RequestMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/g;
  while ((match = reqMapRegex.exec(source)) !== null) {
    result.requestMappings.push({
      method: 'ALL',
      path: match[1],
    });
  }

  // @Autowired 注入
  const autowiredRegex = /@Autowired\s*\n\s*private\s+(\w+)\s+(\w+);/g;
  while ((match = autowiredRegex.exec(source)) !== null) {
    result.autowired.push({
      type: match[1],
      name: match[2],
    });
  }

  // @PreAuthorize("hasRole('XXX')") 或 @PreAuthorize("hasAuthority('XXX')")
  const preAuthMatch = source.match(/@PreAuthorize\s*\(\s*["']has(?:Role|Authority)\s*\(\s*['"]([^'"]+)['"]/);
  if (preAuthMatch) {
    result.preAuthorize = preAuthMatch[1];
  }

  // @OperateLog("描述")
  const opLogMatch = source.match(/@OperateLog\s*\(\s*["']([^"']+)["']/);
  if (opLogMatch) {
    result.operateLog = opLogMatch[1];
  }

  // @TableName("xxx") 或 @TableName(value = "xxx")
  const tableMatch = source.match(/@TableName\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);
  if (tableMatch) {
    result.tableName = tableMatch[1];
  }

  return result;
}

function extractMethodName(source, annotationIndex) {
  // 在注解之后找 public 方法名
  const afterAnnotation = source.substring(annotationIndex);
  const methodMatch = afterAnnotation.match(/public\s+\w+\s+(\w+)\s*\(/);
  return methodMatch ? methodMatch[1] : '';
}

function extractServiceMethodCalls(source, serviceName) {
  // 在方法体中找 serviceName.methodName() 调用
  const calls = [];
  const regex = new RegExp(serviceName + '\\.(\\w+)\\s*\\(', 'g');
  let match;
  while ((match = regex.exec(source)) !== null) {
    calls.push(match[1]);
  }
  return [...new Set(calls)]; // 去重
}

function extractMapperMethodCalls(source, mapperName) {
  const calls = [];
  const regex = new RegExp(mapperName + '\\.(\\w+)\\s*\\(', 'g');
  let match;
  while ((match = regex.exec(source)) !== null) {
    calls.push(match[1]);
  }
  return [...new Set(calls)];
}

// ============ 主扫描逻辑 ============

function scanModule(moduleName) {
  const moduleDir = path.join(BACKEND_ROOT, moduleName);
  const srcDir = path.join(moduleDir, 'src', 'main', 'java');
  
  if (!fs.existsSync(srcDir)) {
    console.log(`  [${moduleName}] 跳过（无源码目录）`);
    return { controllers: [], services: {}, mappers: {} };
  }

  const javaFiles = findAllJavaFiles(srcDir);
  const controllers = [];
  const services = {};
  const mappers = {};

  console.log(`  [${moduleName}] 扫描 ${javaFiles.length} 个 Java 文件`);

  for (const filePath of javaFiles) {
    const source = readFileSafe(filePath);
    const annotations = extractAnnotations(source);
    const fileName = path.basename(filePath, '.java');

    // 识别 Controller
    if (fileName.endsWith('Controller')) {
      for (const mapping of annotations.requestMappings) {
        const endpoint = new Endpoint();
        endpoint.httpMethod = mapping.method;
        endpoint.apiPath = mapping.path;
        endpoint.controller = annotations.className;
        endpoint.permission = annotations.preAuthorize;
        endpoint.operateLog = annotations.operateLog;

        // 找到对应的 Service 注入
        for (const autowired of annotations.autowired) {
          if (autowired.type.endsWith('Service')) {
            endpoint.services.push(autowired.type);
            endpoint.serviceCalls.push(...extractServiceMethodCalls(source, autowired.name));
          }
        }

        controllers.push(endpoint);
      }
    }

    // 识别 Service
    if (fileName.endsWith('Service') || fileName.endsWith('ServiceImpl')) {
      const serviceInfo = {
        className: annotations.className,
        mappers: [],
        mapperMethods: {},
      };

      for (const autowired of annotations.autowired) {
        if (autowired.type.endsWith('Mapper')) {
          serviceInfo.mappers.push(autowired.type);
          serviceInfo.mapperMethods[autowired.type] = extractMapperMethodCalls(source, autowired.name);
        }
      }
      services[annotations.className] = serviceInfo;
    }

    // 识别 Mapper
    if (fileName.endsWith('Mapper')) {
      mappers[annotations.className] = {
        tableName: annotations.tableName,
        className: annotations.className,
      };
    }
  }

  return { controllers, services, mappers };
}

// ============ 生成 Mermaid 时序图 ============

function generateSequenceDiagram(endpoint, services, mappers) {
  const lines = [];
  const participants = new Set();

  participants.add('前端');
  participants.add(endpoint.controller);

  for (const svc of endpoint.services) {
    participants.add(svc);
  }

  const mapperList = [];
  for (const svc of endpoint.services) {
    const serviceInfo = services[svc];
    if (serviceInfo) {
      for (const mapperName of serviceInfo.mappers) {
        participants.add(mapperName);
        mapperList.push(mapperName);
        const mapperInfo = mappers[mapperName];
        if (mapperInfo && mapperInfo.tableName) {
          participants.add(mapperInfo.tableName);
        }
      }
    }
  }

  lines.push('```mermaid');
  lines.push('sequenceDiagram');

  // 声明参与者
  const participantNames = Array.from(participants);
  lines.push(`    participant F as ${participantNames[0] || '前端'}`);
  for (let i = 1; i < participantNames.length; i++) {
    lines.push(`    participant P${i} as ${participantNames[i]}`);
  }

  const pIdx = (name) => {
    const idx = participantNames.indexOf(name);
    if (idx === 0) return 'F';
    return 'P' + idx;
  };

  lines.push('');
  lines.push(`    Note over F: ${endpoint.httpMethod} ${endpoint.apiPath}`);
  if (endpoint.permission) {
    lines.push(`    Note over ${pIdx(endpoint.controller)}: 🔒 ${endpoint.permission}`);
  }
  if (endpoint.operateLog) {
    lines.push(`    Note over ${pIdx(endpoint.controller)}: @OperateLog("${endpoint.operateLog}")`);
  }

  // 前端 → Controller
  lines.push(`    F->>${pIdx(endpoint.controller)}: ${endpoint.httpMethod} ${endpoint.apiPath}`);

  // Controller → Service
  for (const svc of endpoint.services) {
    const calls = endpoint.serviceCalls.length > 0 ? endpoint.serviceCalls.join('/') : '调用';
    lines.push(`    ${pIdx(endpoint.controller)}->>${pIdx(svc)}: ${calls}`);
  }

  // Service → Mapper → Table
  for (const svc of endpoint.services) {
    const serviceInfo = services[svc];
    if (serviceInfo) {
      for (const mapperName of serviceInfo.mappers) {
        const methods = serviceInfo.mapperMethods[mapperName] || [];
        const methodStr = methods.length > 0 ? methods.join('/') : 'CRUD';
        lines.push(`    ${pIdx(svc)}->>${pIdx(mapperName)}: ${methodStr}`);
        
        const mapperInfo = mappers[mapperName];
        if (mapperInfo && mapperInfo.tableName) {
          lines.push(`    Note right of ${pIdx(mapperName)}: 📖✏️ ${mapperInfo.tableName}`);
        }
      }
    }
  }

  lines.push('```');
  return lines.join('\n');
}

// ============ 生成 Markdown 文件 ============

function generateMarkdown(moduleName, moduleControllers, services, mappers) {
  if (moduleControllers.length === 0) return '';

  const lines = [];
  lines.push(`# ${moduleName} 模块（自动生成）\n`);
  lines.push('> ⚠️ 此文件由 `scripts/generate-flow-diagrams.cjs` 自动生成，请勿手动编辑。\n');
  lines.push(`> 生成时间：${new Date().toISOString()}\n`);

  // 按路径分组
  const grouped = {};
  for (const ep of moduleControllers) {
    const group = ep.apiPath.split('/')[2] || 'root'; // /api/v1/{group}
    if (!grouped[group]) grouped[group] = [];
    grouped[group].push(ep);
  }

  for (const [group, endpoints] of Object.entries(grouped)) {
    lines.push(`## ${group}\n`);
    for (const ep of endpoints) {
      lines.push(`### ${ep.httpMethod} ${ep.apiPath}\n`);
      const diagram = generateSequenceDiagram(ep, services, mappers);
      lines.push(diagram);
      lines.push('');
      
      if (ep.services.length > 0) {
        lines.push('**调用链**：');
        const chain = ['前端', ep.controller, ...ep.services];
        lines.push('`' + chain.join(' → ') + '`\n');
      }
    }
  }

  return lines.join('\n');
}

// ============ 主流程 ============

function main() {
  console.log('=== 开始扫描后端代码生成流程图 ===\n');

  if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  }

  let totalEndpoints = 0;

  for (const moduleName of MODULES) {
    const { controllers, services, mappers } = scanModule(moduleName);
    if (controllers.length === 0) continue;

    console.log(`  [${moduleName}] 发现 ${controllers.length} 个端点`);

    const markdown = generateMarkdown(moduleName, controllers, services, mappers);
    if (markdown) {
      const outputPath = path.join(OUTPUT_DIR, `${moduleName}.md`);
      fs.writeFileSync(outputPath, markdown, 'utf8');
      console.log(`  → 输出: ${outputPath}`);
      totalEndpoints += controllers.length;
    }
  }

  console.log(`\n=== 完成！共扫描 ${totalEndpoints} 个端点 ===`);
  console.log(`输出目录: ${OUTPUT_DIR}`);
  console.log('\n提示：');
  console.log('  - 自动生成的文件位于 flowcharts/generated/ 目录');
  console.log('  - 手写流程图（01-auth.md ~ 10-tables.md）不会被覆盖');
  console.log('  - 建议在 CI 中运行此脚本，确保文档与代码同步');
}

main();