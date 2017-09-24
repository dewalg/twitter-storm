
root = [];
var bleed = 100,
    width = 570,
    height = 450;

var pack = d3.layout.pack()
    .sort(null)
    .size([width, height + bleed * 2])
    .padding(2)
    // .sort( function(a, b) {
    // return -(a.value - b.value);})
;
var zoom = d3.behavior.zoom()
    .scaleExtent([0.5, 10])
    .on("zoom", redraw)
;
var svg = d3.select("#HTchart").append("svg")
    .attr("width", width)
    .attr("height", height)
    .call(zoom)
    .append("g")
    .attr("transform", "translate(0," + -bleed + ")")
;
var tooltip = d3.select("#HTchart")
    .append("div")
    .style("position", "absolute")
    .style("z-index", "10")
    .style("visibility", "hidden")
    .style("color", "white")
    .style("padding", "8px")
    .style("background-color", "rgba(0, 0, 0, 0.75)")
    .style("border-radius", "6px")
    .style("font", "12px sans-serif")
    .text("tooltip")
;

function redraw() {
    svg.attr("transform", "translate(" + d3.event.translate + ")" + " scale(" + d3.event.scale + ")");
}

var node = svg.selectAll(".node")
    .data(pack.nodes(flatten(root))
    .filter(function(d) { return !d.children; }))
    .enter().append("g")
    .attr("class", "node")
    .attr("active", "false")
    .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; })
;

node.append("circle")
    .attr("r", function(d) { return d.r; })
    .on("mouseover", function(d) {
          tooltip.text(d.value);
          tooltip.style("visibility", "visible");
  })
  .attr("name", function(d) { return d.name; })
  .attr("active", "false")
  .on("mousemove", function(d) {
      return tooltip.style("top", (d3.event.pageY-330)+"px").style("left",(d3.event.pageX-325)+"px");
  })
  .on("mouseout", function(){return tooltip.style("visibility", "hidden");})
  .on("dblclick",clickOnCircleFunc)
;

node.append("text")
    .attr("id", "httext")
    .text(function(d) { return d.name; })
    .style("font-size", function(d) { return Math.min(2 * d.r, (2 * d.r - 8) / this.getComputedTextLength() * 24) + "px"; })
    .attr("dy", ".35em")
    .on("dblclick",clickOnCircleFunc)
;

function clickOnCircleFunc() {
    d3.event.preventDefault();
    d3.event.stopPropagation();

    if (d3.select(this).attr("active") == null) {
        d3.select(this).attr("active", "false");
    }

    if (d3.select(this).attr("active") == "false") {
         d3.select(this).attr("active", "true");
         global_watchlist.push(d3.select(this).attr("name"));
    } else {
        d3.select(this).attr("active", "false");
        global_watchlist.splice(global_watchlist.indexOf(d3.select(this).attr("name")), 1);
    }

    d3.select(this).style("fill", function(d) { return setStyle(d.name);});
    socket.emit("watchlist", global_watchlist);
}

function setActiveState(name) {
  var x = global_watchlist.indexOf(name);
  if (x == -1) {
      return 'false';
  } else {
      return 'true';
  }
}

function setStyle(name) {
  var x = global_watchlist.indexOf(name);
  if (x == -1) {
      return '#003366';
  } else {
      return '#000000';
  }
}

// Returns a flattened hierarchy containing all leaf nodes under the root.
function flatten(root) {
  var nodes = [];

  function recurse(node) {
    if (node.children) node.children.forEach(recurse);
    else nodes.push({name: node.name, value: node.size});
  }

  recurse(root);
  return {children: nodes};
  }

d3.select(self.frameElement).style("height", height + "px");

function changebubble(root) {
    var node = svg.selectAll(".node")
        .data(pack.nodes(flatten(root))
        .filter(function (d){return !d.children;})
        );
    // capture the enter selection
    var nodeEnter = node
        .enter()
        .append("g")
        .attr("class", "node")
        .attr("active", "false")
        .attr("transform", function (d) {
            return "translate(" + d.x + "," + d.y + ")";})
;

    // re-use enter selection for circles
    nodeEnter
        .append("circle")
        .attr("r", function (d) {return d.r;})
        .on("dblclick",clickOnCircleFunc)
        .on("mouseover", function(d) {
            tooltip.text(d.value);
            tooltip.style("visibility", "visible");})

        .on("mousemove", function(d) {
            return tooltip.style("top", (d3.event.pageY-330)+"px").style("left",(d3.event.pageX-325)+"px");})
        .on("mouseout", function(){return tooltip.style("visibility", "hidden");})
;

    // re-use enter selection for titles
    nodeEnter
        .append("text")
        .text(function (d) { return d.name; })
        .style("font-size", function(d) { return Math.min(2 * d.r, (2 * d.r - 8) / this.getComputedTextLength() * 20) + "px"; })
        .attr("dy", ".35em")
        .on("dblclick",clickOnCircleFunc)
;

    node.select("circle")
        .transition().duration(50)
        .attr("name", function(d) { return d.name; })
        .attr("active", function(d) { return setActiveState(d.name); })
        .attr("r", function (d) { return d.r; })
        .style("fill", function(d) { return setStyle(d.name); })
;

    node.select("text")
        .attr("id", "httext")
        .text(function(d) { return d.name; })
        .transition().duration(50)
        .style("font-size", function(d) {
            var n = d.name;
            var len = n.length+4
            var size = d.r/3;
            size *= 10 / len;
            size += 1;
            return Math.round(size)+'px'; })
        .attr("dy", ".35em")
;

    node.transition().attr("class", "node")
        .attr("transform", function (d) {
            return "translate(" + d.x + "," + d.y + ")"; })
;
    node.exit().remove();

    // Returns a flattened hierarchy containing all leaf nodes under the root.
    function classes(root) {
      var classes = [];

      function recurse(name, node) {
        if (node.children) node.children.forEach(function(child) { recurse(node.name, child); });
        else classes.push({packageName: name, className: node.name, value: node.size});
      }

      recurse(null, root);
      return {children: classes};
    }
}
