cBoard.controller('renderCtrl', function ($timeout, $rootScope, $scope, $state, $location, $http, ModalUtils, chartService) {

    $scope.loading = true;
    $scope.l = 1;
    $scope.persistFinish = false;

    var buildRender = function (w, reload) {
        w.render = function (content, optionFilter, scope) {
            w.persist = {};
            var chartType = w.widget.data.config.chart_type;
            if(chartType == 'chinaMapBmap'){
                chartService.render(content, w.widget.data, optionFilter, scope, reload, w.persist, w.relations);
                w.loading = false;
                $scope.l--;
            } else {
                chartService.render(content, w.widget.data, optionFilter, scope, reload, w.persist, w.relations).then(function (d) {
                    w.realTimeTicket = d;
                    w.loading = false;
                    $scope.l--;
                }, function (error) {
                    $scope.l--;
                });
            }
        };
    };

    $scope.$watch('l', function (newValue) {
        console.log(newValue);
        if (newValue == 0) {
            $timeout(function () {
                runTask();
            }, 3000);
        }
    });

    var runTask = function () {
        var result = {};
        _.each($scope.board.layout.rows, function (row) {
            _.each(row.widgets, function (widget) {
                result[widget.widgetId] = widget.persist;
            });
        });

        html2canvas($('body')[0], {
            onrendered: function (canvas) {
                result['img'] = canvas.toDataURL("image/jpeg");
                var obj = {
                    persistId: $location.search().pid,
                    data: result
                };
                var xmlhttp = new XMLHttpRequest();
                xmlhttp.open("POST", "commons/persist.do", false);
                xmlhttp.send(angular.toJson(obj));
                $scope.$apply(function () {
                    $scope.persistFinish = true;
                });
            }
        });
    };

    $scope.load = function (reload) {
        $scope.loading = true;

        if ($scope.board) {
            _.each($scope.board.layout.rows, function (row) {
                _.each(row.widgets, function (widget) {
                    widget.show = false;
                });
            });
        }
        $http.get("dashboard/getBoardData.do?id=" + $location.search().id).success(function (response) {
            $scope.loading = false;
            $scope.board = response;
            _.each($scope.board.layout.rows, function (row) {
                _.each(row.widgets, function (widget) {
                    if (!_.isUndefined(widget.hasRole) && !widget.hasRole) {
                        return;
                    }
                    buildRender(widget, reload);
                    widget.loading = true;
                    widget.show = true;
                    $scope.l++;
                });
            });
            $scope.l--;
        });
    };
    $scope.load(false);
    
    var paramToFilter = function () {
        $scope.widgetFilters = [];
        $scope.datasetFilters = [];
        $scope.relationFilters = [];

        //将点击的参数赋值到看板上的参数中
        //"{"targetId":3,"params":[{"targetField":"logo","value":"iphone"},{"targetField":"logo1","value":"上海市"}]}" targetField==param.name
//        if(location.href.split("?")[1]) {
        if(false) {
            var urlParam = JSON.parse(decodeURI(location.href.split("?")[1]));
            _.each($scope.board.layout.rows, function (row) {
                _.each(row.params, function (param) {
                    var p = _.find(urlParam.params, function (e) {
                        return e.targetField == param.name;
                    });
                    if(p){
                        param.values.push(p.value);
                    }
                });
            });
            location.href = location.href.split("?")[0];
        }

        _.each($scope.board.layout.rows, function (row) {
            _.each(row.params, function (param) {
                if (param.values.length <= 0) {
                    return;
                }
                _.each(param.col, function (col) {
                    var p = {
                        col: col.column,
                        type: param.type,
                        values: param.values
                    };
                    if (_.isUndefined(col.datasetId)) {
                        if (!$scope.widgetFilters[col.widgetId]) {
                            $scope.widgetFilters[col.widgetId] = [];
                        }
                        $scope.widgetFilters[col.widgetId].push(p);
                    } else {
                        if (!$scope.datasetFilters[col.datasetId]) {
                            $scope.datasetFilters[col.datasetId] = [];
                        }
                        $scope.datasetFilters[col.datasetId].push(p);
                    }
                });
            });
        });
        updateParamTitle();
        //将点击的参数赋值到relationFilters中
        if(_.isUndefined($("#relations").val())){
            return;
        }
        var relations = JSON.parse($("#relations").val());
        for(var i=0;i<relations.length;i++){
            if(relations[i].targetId && relations[i].params && relations[i].params.length>0){
                for(var j=0;j<relations[i].params.length;j++) {
                    var p = {
                        col: relations[i].params[j].targetField,
                        type: "=",
                        values: [relations[i].params[j].value]
                    };
                    if (!$scope.relationFilters[relations[i].targetId]) {
                        $scope.relationFilters[relations[i].targetId] = [];
                    }
                    $scope.relationFilters[relations[i].targetId].push(p); //relation.targetId == widgetId
                }
            }
        }
    };
    
    var updateParamTitle = function () {
        _.each($scope.board.layout.rows, function (row) {
            _.each(row.params, function (param) {
                if ('slider' == param.paramType) {
                    return;
                }
                var paramObj;
                switch (param.type) {
                    case '=':
                    case '≠':
                        paramObj = param.name + ' ' + param.type + ' (' + param.values + ')';
                        break;
                    case '>':
                    case '<':
                    case '≥':
                    case '≤':
                        paramObj = param.name + ' ' + param.type + ' ' + param.values;
                        break;
                    case '(a,b]':
                    case '[a,b)':
                    case '(a,b)':
                    case '[a,b]':
                        var leftBrackets = param.type.split('a')[0];
                        var rightBrackets = param.type.split('b')[1];
                        paramObj = param.name + ' between ' + leftBrackets + param.values[0] + ',' + param.values[1] + rightBrackets;
                        break;
                }
                param.title = param.values.length > 0 ? paramObj : undefined;
            });
        });
    }
    
    var injectFilter = function (widget) {
        var boardFilters = [];
        if(!_.isUndefined($scope.widgetFilters[widget.id])){
            _.each($scope.widgetFilters[widget.id], function(e){
                boardFilters.push(e);
            });
        }
        if(!_.isUndefined($scope.datasetFilters[widget.data.datasetId])){
            _.each($scope.datasetFilters[widget.data.datasetId], function(e){
                boardFilters.push(e);
            });
        }
        if(!_.isUndefined($scope.relationFilters[widget.id])){
            _.each($scope.relationFilters[widget.id], function(e){
                boardFilters.push(e);
            });
        }
        widget.data.config.boardFilters = boardFilters;
        return widget;
    };
    
    $scope.reload = function (widget) {
        paramToFilter();
        widget.widget.data = injectFilter(widget.widget).data;
        widget.show = false;
        widget.showDiv = true;
        widget.render = function (content, optionFilter, scope) {
            //百度地图特殊处理
            var charType = widget.widget.data.config.chart_type;
            if(charType == 'chinaMapBmap'){
                chartService.render(content, widget.widget.data, optionFilter, scope, true);
                widget.loading = false;
            } else {
                chartService.render(content, widget.widget.data, optionFilter, scope, true, null, widget.relations).then(function (d) {
                    widget.realTimeTicket = d;
                    widget.loading = false;
                });
            }
            widget.realTimeOption = {optionFilter: optionFilter, scope: scope};
        };
        $timeout(function () {
            widget.loading = true;
            widget.show = true;
        });
    };
});