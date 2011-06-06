/*!
 * jppf script
 */

function anim_main2()
{
	$("#slidetabs").tabs("#images > div", {

		// enable "cross-fading" effect
		effect: 'fade',
		//effect: 'jppfeffect2',
		fadeInSpeed: 3000,
		fadeOutSpeed: 3000,

		// start from the beginning after the last tab
		rotate: true

	// use the slideshow plugin. It accepts its own configuration
	}).slideshow({autoplay: true, interval: 8000});
	$("#slidetabs").data("slideshow").play();
}

var w;
var p;
var pane;
/**
 * Horizontal accordion
 * 
 * @deprecated will be replaced with a more robust implementation
 */
$.tools.tabs.addEffect("jppfslide", function(i, done) {

	// store original width of a pane into memory
	//if (!w) { w = this.getPanes().eq(0).width(); }
	if (!p)
	{
		pane = this.getPanes().eq(0);
		pane.show(); 
		w = pane.width();
		p = pane.offset();
		pane.hide(); 
	}

	//this.getCurrentPane().hide(2000, 'linear');
	// set current pane's width to zero
	this.getCurrentPane().animate({left: p.left+w, width: 0}, 2000, 'linear', function() { $(this).hide(); });

  pane = this.getPanes().eq(i);
	pane.width(0);
	pane.show();
	pane.offset({left: p.left, top: p.top});
	// grow opened pane to its original width
	pane.animate({width: w}, 2600, 'linear', function() { done.call();	});
});	


/**
 * Horizontal accordion
 * 
 * @deprecated will be replaced with a more robust implementation
 */
$.tools.tabs.addEffect("jppfeffect2", function(i, done)
{
	this.getCurrentPane().hide(3000, 'swing');
	this.getPanes().eq(i).show(3000, 'swing', function() { done.call(); });
});	
