javascript: (function(root, ev) {
    function Brige() {
        this.init = function(evn) {
            this.version = evn.version
        };
        this.callNative = function(url) {
            var t = document.createElement("iframe");
            t.style.width = "1px";
            t.style.height = "1px";
            t.style.display = "none";
            t.src = url;
            document.body.appendChild(t);
            setTimeout(function() {
                document.body.removeChild(t),
                t.remove()
            },
            100)
        };
        this.saveToSDK = function(urls) {
            this.callNative("mmaViewabilitySDK://saveJSCacheData?data=" + JSON.stringify(urls))
        };
        this.stop = function(viewabilityID) {
            this.callNative("mmaViewabilitySDK://stopViewability?AdviewabilityID=" + viewabilityID)
        };
        this.sendViewabilityMessage = function(string) {}
    }
    if ("undefined" === typeof root.MMASDK) {
        root.MMASDK = new Brige();
        root.MMASDK.init(ev);
        root.__MMASDKInit = true
    }
})(window, {
    "version": "1.0"
});
