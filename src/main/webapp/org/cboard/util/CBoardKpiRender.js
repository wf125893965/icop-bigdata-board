var CBoardKpiRender = function (jqContainer, options) {
    this.container = jqContainer; // jquery object
    this.options = options;
};

CBoardKpiRender.prototype.html = function (persist) {
    var self = this;
    var temp = "" + self.template;
    var html = temp.render(self.options);
    if (persist) {
        setTimeout(function () {
            self.container.css('background', '#fff');
            html2canvas(self.container, {
                onrendered: function (canvas) {
                    persist.data = canvas.toDataURL("image/jpeg");
                    persist.type = "jpg"
                }
            });
        }, 1000);
        // persist.data = {name: self.options.kpiName, value: self.options.kpiValue};
        // persist.type = "kpi";
    }
    return html;
};

CBoardKpiRender.prototype.realTimeTicket = function () {
    var self = this;
    return function (o) {
        $(self.container).find('h3').html(o.kpiValue);
    }
};

CBoardKpiRender.prototype.do = function () {
    var self = this;
    $(self.container).html(self.rendered());
};

CBoardKpiRender.prototype.template =
    "<div class='small-box {style}'> \
               <div class='inner'> \
                   <h3>{kpiValue}</h3> \
                   <p>{kpiName}</p> \
               </div> \
               <div class='icon'> \
                   <i class='ion ion-stats-bars'></i> \
               </div> \
               <a class='small-box-footer' style='font-size: 14px;'>\
                   <span ng-click='reload(widget)' style='cursor: pointer; margin-right: 20%;'>{refresh} <i class='iconfont icon-xingzhuang' style='font-size: 14px;'></i></span>\
                   <span ng-click='config(widget)' ng-if='widgetCfg' style='cursor: pointer; margin-left: 20%;'>{edit} <i class='iconfont icon-bianji' style='font-size: 14px;'></i></span>\
               </a>\
            </div>";
