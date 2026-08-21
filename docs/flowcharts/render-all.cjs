// 批量渲染 Mermaid .mmd 文件为 PNG（使用 kroki.io POST API）
// 用法: node docs/flowcharts/render-all.cjs
//       或 node docs/flowcharts/render-all.cjs 01-总体架构.mmd
const fs = require('fs');
const https = require('https');
const path = require('path');

const FLOWCHARTS_DIR = __dirname;

function renderOne(mmdFile) {
    return new Promise((resolve, reject) => {
        const mmdPath = path.join(FLOWCHARTS_DIR, mmdFile);
        const svgPath = mmdPath.replace(/\.mmd$/, '.svg');
        
        const mmd = fs.readFileSync(mmdPath, 'utf8');
        console.log(`  [${mmdFile}] ${mmd.length} chars → ${svgPath}`);
        
        const body = JSON.stringify({
            diagram_source: mmd,
            diagram_type: 'mermaid',
            output_format: 'svg'
        });
        
        const options = {
            hostname: 'kroki.io',
            path: '/mermaid/svg',
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(body)
            }
        };
        
        const req = https.request(options, (res) => {
            if (res.statusCode === 200) {
                const chunks = [];
                res.on('data', (c) => chunks.push(c));
                res.on('end', () => {
                    const buf = Buffer.concat(chunks);
                    fs.writeFileSync(svgPath, buf);
                    console.log(`  ✓ ${buf.length} bytes`);
                    resolve();
                });
            } else {
                let respBody = '';
                res.on('data', (c) => respBody += c);
                res.on('end', () => {
                    console.log(`  ✗ ${res.statusCode}: ${respBody.substring(0, 200)}`);
                    reject(new Error(`${mmdFile}: HTTP ${res.statusCode}`));
                });
            }
        });
        
        req.on('error', (e) => {
            console.log(`  ✗ ${e.message}`);
            reject(e);
        });
        
        req.setTimeout(30000, () => { req.destroy(); reject(new Error('timeout')); });
        req.write(body);
        req.end();
    });
}

async function main() {
    const target = process.argv[2];
    
    let files;
    if (target) {
        files = [target];
    } else {
        files = fs.readdirSync(FLOWCHARTS_DIR)
            .filter(f => f.endsWith('.mmd'))
            .sort();
    }
    
    if (files.length === 0) {
        console.log('No .mmd files found in', FLOWCHARTS_DIR);
        return;
    }
    
    console.log(`Rendering ${files.length} diagram(s)...\n`);
    
    let ok = 0, fail = 0;
    for (const f of files) {
        try {
            await renderOne(f);
            ok++;
        } catch (e) {
            fail++;
        }
    }
    
    console.log(`\nDone: ${ok} OK, ${fail} failed`);
    process.exit(fail > 0 ? 1 : 0);
}

main();