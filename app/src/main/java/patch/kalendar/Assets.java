package patch.kalendar;

/** CSS i JS koji se ubacuju u WebView stranice aplikacije (tamna tema). */
final class Assets {
    private Assets() {}

    private static String hex(int c) {
        return "#" + Colors.hex(c);
    }

    /** CSS za tamnu temu; boje dolaze iz Dark.* (podrazumijevane ili prilagodjene). */
    static String darkCss() {
        return ":root{color-scheme:" + (Dark.light() ? "light" : "dark") + "}\n"
                + "html,body{background:" + hex(Dark.WEB_BG) + " !important;background-image:none !important;color:" + hex(Dark.WEB_BODY) + " !important}\n"
                + "#wrapper,div.transbox,.calibre,.calibre1{background:transparent !important;background-image:none !important}\n"
                + "h1,h2,h3,h4,h5,h6,.title,.naslov,.color-red{color:" + hex(Dark.TITLE) + " !important}\n"
                + "h5,h6{color:" + hex(Dark.WEB_HEAD2) + " !important}\n"
                + "a,a:link{color:" + hex(Dark.WEB_LINK) + "}\n"
                + "a:visited{color:" + hex(Dark.WEB_VISITED) + "}\n"
                + "a:hover{color:" + hex(Dark.TITLE) + " !important}\n"
                + "hr{border-color:" + hex(Dark.WEB_LINE) + " !important;color:" + hex(Dark.WEB_LINE) + " !important;background-color:" + hex(Dark.WEB_LINE) + "}\n"
                + "table,td,th{border-color:" + hex(Dark.WEB_LINE) + "}\n"
                + "input,select,textarea,button{background:" + hex(Dark.SURFACE) + ";color:" + hex(Dark.WEB_BODY) + ";border-color:" + hex(Dark.WEB_LINE) + "}\n"
                + "[data-dkb][data-dkb][data-dkb],[data-dkb][data-dkb][data-dkb]:link,[data-dkb][data-dkb][data-dkb]:visited,[data-dkb][data-dkb][data-dkb]:hover,[data-dkb][data-dkb][data-dkb]:active,[data-dkb][data-dkb][data-dkb]:focus{background:" + hex(Dark.BTN_BG) + " !important;background-image:none !important;color:" + hex(Dark.BTN_TEXT) + " !important;border-color:" + hex(Dark.BTN_BORDER) + " !important;outline-color:" + hex(Dark.BTN_BORDER) + " !important;text-shadow:none !important}\n"
                + "[data-dkb][data-dkb] *{color:" + hex(Dark.BTN_TEXT) + " !important}\n"
                + "::-webkit-scrollbar-track{-webkit-box-shadow:inset 0 0 3px rgba(0,0,0," + (Dark.light() ? ".25" : ".8") + ")}\n"
                + Fonts.webCss();
    }

    /** Pretvara tekst u \\uXXXX znakove (ASCII), da se moze ubaciti u stranicu bilo kojeg kodiranja. */
    private static String u(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 128) b.append(c); else b.append(String.format("\\u%04x", (int) c));
        }
        return b.toString();
    }

    /** Zajednicki dio skripti: prepoznavanje i bojenje dugmadi (претходно / календар / садржај / следеће...). */
    private static final String[] NAV_WORDS = {
            "претходно", "претходна",
            "следеће", "следећа",
            "календар", "садржај",
    };

    private static String navWordsJs() {
        StringBuilder b = new StringBuilder("var W={");
        for (int i = 0; i < NAV_WORDS.length; i++) {
            if (i > 0) b.append(',');
            b.append('\'').append(u(NAV_WORDS[i])).append("':1");
        }
        b.append("};");
        return b.toString();
    }

    private static final String NAV_CORE =
            "var BL={TD:1,TH:1,TR:1,TABLE:1,TBODY:1,THEAD:1,TFOOT:1,LI:1,UL:1,OL:1,P:1,FORM:1,BODY:1,HTML:1,CENTER:1,SECTION:1,ARTICLE:1,MAIN:1,HEADER:1,FOOTER:1,NAV:1};"
            + "var IT={A:1,BUTTON:1,INPUT:1};"
            + "function isNav(e){try{if(e.nodeType!==1)return false;if(e.childElementCount>3)return false;var r=(e.value||e.textContent||'');if(r.length>400)return false;var t=r.replace(/[<>\\[\\]\\u00ab\\u00bb\\u2039\\u203a\\u2190\\u2192\\u25c4\\u25ba]/g,'').replace(/\\s+/g,' ').trim().toLowerCase();if(W[t]===1)return true;}catch(x){}return false;}"
            + "function looks(p){try{var c=getComputedStyle(p);return c.backgroundImage!=='none'||parseFloat(c.borderTopWidth)>0||parseFloat(c.borderTopLeftRadius)>0||c.boxShadow!=='none';}catch(x){return false;}}"
            + "function isBtn(e){if(!isNav(e))return false;var t=e.tagName;if(BL[t])return false;if(IT[t])return true;return looks(e);}"
            + "function mark(e){try{e.setAttribute('data-dkb','1');var s=e.style;s.setProperty('background-color','@BTNBG@','important');s.setProperty('background-image','none','important');s.setProperty('color','@BTNTEXT@','important');s.setProperty('border-color','@BTNBORDER@','important');s.setProperty('outline-color','@BTNBORDER@','important');}catch(x){}}"
            + "function scan(r){try{if(!r||r.nodeType!==1)return;if(CS)csScan(r);if(isBtn(r))mark(r);var l=r.querySelectorAll('a,button,input,span,div,b,font,i,strong');for(var i=0;i<l.length;i++)if(isBtn(l[i]))mark(l[i]);}catch(x){}}"
            + "var markNav=scan;";

    /** Crkvenoslovenski fontovi (triodion-ieUcs i IrmolSkun_0): elementi sa takvim fontom dobijaju oznaku data-dkcs (preuzeti font se ne primjenjuje). */
    private static final String CS_JS =
            "var CSF=/triodion|ieucs|irmol|skun/i;"
            + "function tagCs(p){try{if(!p||p.nodeType!==1)return;p.setAttribute('data-dkcs','1');var par=p.parentNode;if(par&&par.nodeType===1&&par.tagName!=='BODY'&&par.tagName!=='HTML'){par.setAttribute('data-dkcs','1');}var ch=p.children;for(var i=0;i<ch.length;i++)tagCs(ch[i]);}catch(x){}}"
            + "function isCsNode(n,p){try{if(p&&p.nodeType===1){if(p.hasAttribute&&p.hasAttribute('data-dkcs'))return true;var face=p.getAttribute&&p.getAttribute('face');if(face&&CSF.test(face))return true;var style=p.getAttribute&&p.getAttribute('style');if(style&&CSF.test(style))return true;var cls=p.className;if(typeof cls==='string'&&CSF.test(cls))return true;var f=p.style&&p.style.fontFamily;if(f&&CSF.test(f))return true;var cf=getComputedStyle(p).fontFamily;if(cf&&CSF.test(cf))return true;}}catch(x){}return false;}"
            + "function csScan(r){try{if(!r||r.nodeType!==1)return;var w=document.createTreeWalker(r,NodeFilter.SHOW_TEXT,null,false),n;"
            + "while((n=w.nextNode())){var p=n.parentNode;if(isCsNode(n,p))tagCs(p);}}catch(x){}}"
            + "function csText(n){try{var p=n.parentNode;if(isCsNode(n,p))tagCs(p);}catch(x){}}";

    private static String navCore() {
        return "var CS=" + (Prefs.keepSlavonic() ? "true" : "false") + ";" + CS_JS + navWordsJs() + NAV_CORE
                .replace("@BTNBG@", hex(Dark.BTN_BG))
                .replace("@BTNTEXT@", hex(Dark.BTN_TEXT))
                .replace("@BTNBORDER@", hex(Dark.BTN_BORDER));
    }

    private static final String NAV_OBSERVER =
            "function text(n){if(CS)csText(n);var p=n.parentNode;for(var k=0;k<3&&p&&p.nodeType===1;k++,p=p.parentNode)if(isBtn(p))mark(p);}"
            + "try{new MutationObserver(function(ms){for(var i=0;i<ms.length;i++){var m=ms[i];if(m.type==='characterData'){text(m.target);continue;}var a=m.addedNodes;for(var j=0;j<a.length;j++){var n=a[j];if(n.nodeType===1)scan(n);else if(n.nodeType===3)text(n);}}}).observe(document,{childList:true,subtree:true,characterData:true});}catch(x){}"
            + "document.addEventListener('DOMContentLoaded',function(){scan(document.documentElement);});"
            + "window.addEventListener('load',function(){scan(document.documentElement);});"
            + "var dl=[60,250,700,1500,3000];for(var q=0;q<dl.length;q++)setTimeout(function(){scan(document.documentElement);},dl[q]);";

    /**
     * Skripta ubacena u zaglavlje .html stranica: dugmad dobiju oznaku data-dkb cim se pojave u stranici (prije prvog
     * iscrtavanja), pa nema perioda u kome su u originalnim (crvenim) bojama; ponovni pregled hvata kasnije promjene.
     */
    static String navJs() {
        return "(function(){" + navCore() + NAV_OBSERVER + "})();";
    }


    static String darkJs() {
        boolean colors = Prefs.dark();   // false = tema "Православац": samo fontovi, boje stranica se ne diraju
        String css = (colors ? darkCss() : Fonts.webCss()).replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n");
        String markFont = Fonts.cssFamily("c_web_mark");   // font oznaka verzija (zelene oznake) ili null
        String titleFont = Fonts.cssFamily("c_title");     // font naslova ili null
        return DARK_JS.replace("@NAVCORE@", navCore()).replace("@TEXT@", hex(Dark.WEB_BODY)).replace("@TITLE@", hex(Dark.TITLE)).replace("@MARK@", hex(Dark.WEB_MARK))
                .replace("@BG@", hex(Dark.WEB_BG)).replace("@LIGHT@", Dark.light() ? "true" : "false")
                .replace("@MKR@", String.valueOf((Dark.WEB_MARK >> 16) & 255)).replace("@MKG@", String.valueOf((Dark.WEB_MARK >> 8) & 255))
                .replace("@MKB@", String.valueOf(Dark.WEB_MARK & 255))
                .replace("@TITR@", String.valueOf((Dark.TITLE >> 16) & 255)).replace("@TITG@", String.valueOf((Dark.TITLE >> 8) & 255))
                .replace("@TITB@", String.valueOf(Dark.TITLE & 255))
                .replace("@MARKFONT@", markFont == null ? "" : markFont)
                .replace("@MARKZOOM@", Prefs.fontSize("c_web_mark") == 100 ? "" : Prefs.fontSize("c_web_mark") + "%")
                .replace("@TITLEFONT@", titleFont == null ? "" : titleFont)
                .replace("@TITLEZOOM@", Prefs.fontSize("c_title") == 100 ? "" : Prefs.fontSize("c_title") + "%")
                .replace("@COLORS@", colors ? "true" : "false")
                .replace("@NOMARK@", colors ? "" : "function mark(e){}")
                .replace("@CSS@", css);
    }

    static final String DARK_JS =
            "(function(){\n"
                    + "  if(window.__dkRun){window.__dkRun(document.documentElement);return;}\n"
                    + "  @NAVCORE@\n"
                    + "  @NOMARK@\n"
                    + "  if(CS){try{csScan(document.documentElement);}catch(x){}}\n"
                    + "  try{var st=document.getElementById('__dkcss');if(!st){st=document.createElement('style');st.id='__dkcss';(document.head||document.documentElement).appendChild(st);}st.textContent='@CSS@';}catch(x){}\n"
                    + "  var COLORS=@COLORS@;var LIGHT=@LIGHT@;var MARKFONT='@MARKFONT@';var MARKZOOM='@MARKZOOM@';\n"
                    + "  var TITLEFONT='@TITLEFONT@';var TITLEZOOM='@TITLEZOOM@';\n"
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
                    + "    if(x[1]<0.2)return'@TEXT@';\n"
                    + "    if(x[1]>=0.6&&(x[0]<=15||x[0]>=345))return'@TITLE@';\n"
                    + "    if(x[1]>=0.2&&x[0]>=75&&x[0]<=165)return'@MARK@';\n"
                    + "    if(LIGHT)return null;\n"
                    + "    return hsl(x[0],Math.min(1,x[1])*0.9,0.72);\n"
                    + "  }\n"
                    + "  function bgMap(c){\n"
                    + "    if(c.a<0.1)return null;\n"
                    + "    var x=toHsl(c.r,c.g,c.b);\n"
                    + "    if(x[2]<0.6)return null;\n"
                    + "    if(LIGHT)return'@BG@';\n"
                    + "    return hsl(x[0],x[1]*0.35,0.07+(1-x[2])*0.45);\n"
                    + "  }\n"
                    + "  function fix(e){\n"
                    + "    if(e.nodeType!==1||SKIP[e.tagName.toUpperCase()])return;\n"
                    + "    var tn=e.tagName.toUpperCase();\n"
                    + "    if(COLORS&&(tn==='A'||tn==='BUTTON'||tn==='INPUT'||tn==='SPAN'||tn==='DIV')&&isBtn(e)){mark(e);return;}\n"
                    + "    var cs=getComputedStyle(e),c=parse(cs.color),b=parse(cs.backgroundColor),v;\n"
                    + "    if(CS&&(e.hasAttribute('data-dkcs')||e.getAttribute('data-dkcs')==='1'||CSF.test(cs.fontFamily))){e.setAttribute('data-dkcs','1');}\n"
                    + "    if(COLORS&&c&&(v=fgMap(c)))e.style.setProperty('color',v,'important');\n"
                    + "    if(!e.hasAttribute('data-dkcs')&&c&&(TITLEFONT||TITLEZOOM)){\n"
                    + "      var h3=toHsl(c.r,c.g,c.b);\n"
                    + "      if((lum(c)<0.5&&h3[1]>=0.6&&(h3[0]<=15||h3[0]>=345))||(c.r===@TITR@&&c.g===@TITG@&&c.b===@TITB@)){\n"
                    + "        if(TITLEFONT)e.style.setProperty('font-family',TITLEFONT,'important');\n"
                    + "        if(TITLEZOOM)e.style.setProperty('zoom',TITLEZOOM,'important');\n"
                    + "      }\n"
                    + "    }\n"
                    + "    if(!e.hasAttribute('data-dkcs')&&(TITLEFONT||TITLEZOOM)&&(tn==='H1'||tn==='H2'||tn==='H3'||tn==='H4'||tn==='H5'||tn==='H6'||e.classList.contains('title')||e.classList.contains('naslov')||e.classList.contains('color-red'))){\n"
                    + "      if(TITLEFONT)e.style.setProperty('font-family',TITLEFONT,'important');\n"
                    + "      if(TITLEZOOM)e.style.setProperty('zoom',TITLEZOOM,'important');\n"
                    + "    }\n"
                    + "    if(c&&(MARKFONT||MARKZOOM)){var h2=toHsl(c.r,c.g,c.b);if((lum(c)<0.5&&h2[1]>=0.2&&h2[0]>=75&&h2[0]<=165)||(c.r===@MKR@&&c.g===@MKG@&&c.b===@MKB@)){if(MARKFONT)e.style.setProperty('font-family',MARKFONT,'important');if(MARKZOOM)e.style.setProperty('zoom',MARKZOOM,'important');}}\n"
                    + "    if(!COLORS){}else if(e===document.body||e===document.documentElement){e.style.setProperty('background-color','@BG@','important');e.style.setProperty('background-image','none','important');}\n"
                    + "    else if(b&&(v=bgMap(b)))e.style.setProperty('background-color',v,'important');\n"
                    + "    var bi=cs.backgroundImage;\n"
                    + "    if(COLORS&&bi&&bi!=='none'&&/pozadina|fine-grain|paper|diagonal-maze/i.test(bi))e.style.setProperty('background-image','none','important');\n"
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
                    + "      for(var i=0;i<ms.length;i++){var n=ms[i].addedNodes;for(var j=0;j<n.length;j++)if(n[j].nodeType===1){markNav(n[j]);q.push(n[j]);}}\n"
                    + "      if(q.length&&!t)t=setTimeout(function(){var l=q;q=[];t=null;for(var k=0;k<l.length;k++)run(l[k]);},40);\n"
                    + "    });\n"
                    + "    mo.observe(document.documentElement,{childList:true,subtree:true});\n"
                    + "  }catch(x){}\n"
                    + "})();\n";

    /** Lijevo poravnanje za elemente sa inline stilom / atributom (stilovi iz .css fajlova se prepisuju u Hook-u). */
    static final String LEFT_CSS =
            "[align=\"justify\"],[style*=\"justify\"],#just{text-align:left !important}\n";

    static final String JUSTIFY_JS =
            "(function(){var a=document.getElementsByTagName('*');for(var i=0;i<a.length;i++){var e=a[i];if(window.getComputedStyle(e).textAlign==='justify'){e.style.setProperty('text-align','left','important');}}})();";
}
