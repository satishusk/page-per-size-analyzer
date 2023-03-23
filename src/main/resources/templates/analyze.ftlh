<!doctype html>
<html lang="en">
<head>
	<title>Lab2</title>
</head>

<body>
<style>
    body {
        width: min-content;
        height: min-content;
        padding: 5px 10px 5px 10px;
        border: 1px solid #999;
        font-size: 12px;
        font-family: "Consolas", monospace;
    }

    h1 {
        text-align: center;
    }

    .entry {
        width: max-content;
    }
</style>

<h1>Variant III. Recursive page per size analyzer</h1>
<hr>
<form action="/analyze" method="post">
	Depth: <input type="number" name="depth" placeholder="Depth" step="1"
	              value="<#if depth??>${depth}<#else >0</#if>" min="0" max="100"
	              required><br><br>
	URI: <input type="text" name="uri" placeholder="URI"
	            value="<#if depth??>${uri}<#else >http://imc.ssau.ru/</#if>" size="50" required><br><br>
	<input type="submit" value="Analyze"><br><br><br>
</form>

Summary:<br><br>
<#if count?? && summarySize?? && min??>
	1) count: ${count}<br>
	2) summarySize: ${summarySize} bytes <br>
	3) min: ${min} - ${minBytes} bytes <br>
	4) max: ${max} - ${maxBytes} bytes <br><br><br>
</#if>

Analyzed addresses:<br><br>
<#if entries??>
    <#list entries as entry>
        <div class="entry">${entry?counter}) ${entry.uri} - ${entry.size} bytes <br></div>
    </#list>
</#if>

</body>
</html>