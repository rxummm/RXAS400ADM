# 扫描后端服务接口 public 方法中全库零调用方的方法
$ErrorActionPreference = 'SilentlyContinue'
$root = 'D:\vueprojects\RXAS400ADM\backend'
$allJava = Get-ChildItem $root -Recurse -Filter *.java | Where-Object { $_.FullName -notmatch '\\target\\' }

# 读取全部内容一次，避免重复 IO
$contentMap = @{}
foreach ($f in $allJava) {
    $contentMap[$f.FullName] = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
}

$suspects = @()
foreach ($f in $allJava) {
    if ($f.Name -notmatch '^I[A-Z].*Service\.java$') { continue }
    $content = $contentMap[$f.FullName]
    # 接口方法声明：行首无修饰符或 public，形如  XxxYyy returnType name(
    $ms = [regex]::Matches($content, '(?m)^\s*(?:public\s+)?[A-Za-z0-9_<>\[\],\.]+\s+([a-z][A-Za-z0-9_]*)\s*\([^;]*\);\s*$')
    foreach ($m in $ms) {
        $name = $m.Groups[1].Value
        if ($name -in @('toString','hashCode','equals')) { continue }
        $total = 0
        foreach ($kv in $contentMap.GetEnumerator()) {
            $count = ([regex]::Matches($kv.Value, "\b$name\b")).Count
            $total += $count
        }
        # 定义处出现 1 次；<=2 视为可疑（定义+至多1处引用）
        if ($total -le 2) {
            $suspects += "{0}  {1}  (总引用 {2})" -f $f.Name, $name, $total
        }
    }
}
if ($suspects) { $suspects | Sort-Object } else { Write-Output '(无可疑未使用接口方法)' }
