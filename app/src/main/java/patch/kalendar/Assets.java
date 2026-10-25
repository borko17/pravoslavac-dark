package patch.kalendar;

/** CSS i JS koji se ubacuju u WebView stranice aplikacije (tamna tema). */
final class Assets {
    private Assets() {}

    static final String DARK_CSS =
        ":root{color-scheme:dark}\n"
        + "html,body{background:#121212 !important;background-image:none !important;color:#d9d4c7 !important}\n"
        + "#wrapper,div.transbox,.calibre,.calibre1{background:transparent !important;background-image:none !important}\n"
        + "h1,h2,h3,h4,.color-red{color:#ff7a66 !important}\n"
        + "h5,h6{color:#e6e1d6 !important}\n"
        + "a,a:link{color:#8ab4f8}\n"
        + "a:visited{color:#c4a5f0}\n"
        + "a:hover{color:#ff7a66 !important}\n"
        + "hr{border-color:#444 !important;color:#444 !important;background-color:#444}\n"
        + "table,td,th{border-color:#444}\n"
        + "input,select,textarea,button{background:#1e1e1e;color:#d9d4c7;border-color:#555}\n"
        + "::-webkit-scrollbar-track{-webkit-box-shadow:inset 0 0 3px rgba(0,0,0,.8)}\n";

    static final String DARK_JS =
        "(function(){\n"
        + "  if(window.__dkRun){window.__dkRun(document.documentElement);return;}\n"
        + "  var SKIP={SCRIPT:1,STYLE:1,IMG:1,SVG:1,LINK:1,META:1,HEAD:1,TITLE:1,BR:1,HR:1,CANVAS:1,VIDEO:1,AUDIO:1,IFRAME:1,OBJECT:1,EMBED:1,NOSCRIPT:1};\n"
        + "  function parse(c){var m=/rgba?\\(([^)]+)\\)/.exec(c);if(!m)return null;var a=m[1].split(',');return{r:+a[0],g:+a[1],b:+a[2],a:a.length>3?+a[3]:1};}\n"
        + "  function lum(c){return(0.299*c.r+0.587*c.g+0.114*c.b)/255;}\n"
        + "  function toHsl(r,g,b){r/=255;g/=255;b/=255;var mx=Math.max(r,g,b),mn=Math.min(r,g,b),l=(mx+mn)/2,h=0,s=0,d=mx-mn;\n"
        + "    if(d){s=l>0.5?d/(2-mx-mn):d/(mx+mn);if(mx==r)h=(g-b)/d+(g<b?6:0);else if(mx==g)h=(b-r)/d+2;else h=(r-g)/d+4;h*=60;}\n"
        + "    return[h,s,l];}\n"
        + "  function hsl(h,s,l){return'hsl('+Math.round(h)+','+Math.round(s*100)+'%,'+Math.round(l*100)+'%)';}\n"
        + "  function fgMap(c){\n"
        + "    if(lum(c)>=0.5)return null;\n"
        + "    var x=toHsl(c.r,c.g,c.b);\n"
        + "    if(x[1]<0.2)return'#d9d4c7';\n"
        + "    return hsl(x[0],Math.min(1,x[1])*0.9,0.72);\n"
        + "  }\n"
        + "  function bgMap(c){\n"
        + "    if(c.a<0.1)return null;\n"
        + "    var x=toHsl(c.r,c.g,c.b);\n"
        + "    if(x[2]<0.6)return null;\n"
        + "    return hsl(x[0],x[1]*0.35,0.07+(1-x[2])*0.45);\n"
        + "  }\n"
        + "  function fix(e){\n"
        + "    if(e.nodeType!==1||SKIP[e.tagName.toUpperCase()])return;\n"
        + "    var cs=getComputedStyle(e),c=parse(cs.color),b=parse(cs.backgroundColor),v;\n"
        + "    if(c&&(v=fgMap(c)))e.style.setProperty('color',v,'important');\n"
        + "    if(b&&(v=bgMap(b)))e.style.setProperty('background-color',v,'important');\n"
        + "    var bi=cs.backgroundImage;\n"
        + "    if(bi&&bi!=='none'&&/pozadina|fine-grain|paper|diagonal-maze/i.test(bi))e.style.setProperty('background-image','none','important');\n"
        + "  }\n"
        + "  function run(root){\n"
        + "    try{\n"
        + "      if(!root)return;\n"
        + "      fix(root);\n"
        + "      var all=root.getElementsByTagName('*');\n"
        + "      for(var i=0;i<all.length;i++)fix(all[i]);\n"
        + "    }catch(x){}\n"
        + "  }\n"
        + "  window.__dkRun=run;\n"
        + "  run(document.documentElement);\n"
        + "  try{\n"
        + "    var q=[],t=null;\n"
        + "    var mo=new MutationObserver(function(ms){\n"
        + "      for(var i=0;i<ms.length;i++){var n=ms[i].addedNodes;for(var j=0;j<n.length;j++)if(n[j].nodeType===1)q.push(n[j]);}\n"
        + "      if(q.length&&!t)t=setTimeout(function(){var l=q;q=[];t=null;for(var k=0;k<l.length;k++)run(l[k]);},40);\n"
        + "    });\n"
        + "    mo.observe(document.documentElement,{childList:true,subtree:true});\n"
        + "  }catch(x){}\n"
        + "})();\n";

    static final String JUSTIFY_JS =
        "(function(){var a=document.getElementsByTagName('*');for(var i=0;i<a.length;i++){var e=a[i];if(window.getComputedStyle(e).textAlign==='justify'){e.style.setProperty('text-align','left','important');}}})();";
}
