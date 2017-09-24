d3.json("indiaTopoJSON_new.json", function(error, map) {

	d3.select("body").select('#loader_container1').remove();



	function plotty(id, parameter, parameterName, color, correction) {

	  var width = 1000,
	      height = 800;


	  var projection = d3.geo.albers()
	      .scale(1500)
	      .translate([(width/2)-40, height/2])
	      .rotate([-80.0200,0])
	      .center([0,22.1800])

	  var svg = d3.select("body").select(id).append("svg")
	      .attr("width", width)
	      .attr("height", height);


	  var g = svg.append("g");

	  var path = d3.geo.path()
	    .projection(projection);



	  	svg.selectAll(".subunit")
		    .data(topojson.feature(map, map.objects.asasas).features)
		    .enter().append("path")
		    .attr("class", "subunit")
		    .attr("id", function(d) { return d.properties.DISTRICT
		    })
			.attr("d", path)
			.attr("fill", color)
			.attr("opacity", function(d) {
				if (d.properties.DISTRICT == null) {
					return 0
				}
				else {
					return eval(correction)
				}
			})
			.on("mouseover", function() {
				d3.select(this)
			   	  .attr("fill", "orange")
			   	  .attr("opacity", "0.8")
			   	  .append("svg:title")
			   	  .html(function(d){

			   	  	// 'd.properties.' + parameter

			   	  	return "<p>District: " + d.properties.DISTRICT + "</br>" + parameterName + ": " + eval('d.properties.' + parameter) + "</p>"
			   	  })
			})
          	.on("mouseout", function() {
				d3.select(this)
			   	  .attr("fill", color)
			   	  .attr("opacity", function(d) {
                      console.log(d3.select(this).attr("id") + ': ' + eval(correction));
					  return eval(correction)
				  })
				  .select()
				  .remove()
			});

            svg.append("path")
                .datum(topojson.mesh(map, map.objects.asasas))
                .attr("d", path)
                .attr("class", "subunit-boundary");

	}


plotty("#india1", "TOTAL_LIT_PERCENT", "Literacy Rate (in %)", "#058905", " (d.properties.TOTAL_LIT_PERCENT-40)/55 ")
})
