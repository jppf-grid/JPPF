MandelbrotConfiguration createConfig() {
  System.out.println("createConfig(1)")
  def x = option.findFirstWithName("/centerX").getValue();
  System.out.println("createConfig(2)")
  def y = option.findFirstWithName("/centerY").getValue();
  System.out.println("createConfig(3)")
  def d = option.findFirstWithName("/diameter").getValue();
  System.out.println("createConfig(4)")
  def iter = option.findFirstWithName("/iterations").getValue();
  System.out.println("createConfig(5)")
  return new MandelbrotConfiguration(x, y, d, iter.intValue());
}

void computeMandelbrot(config) {
  System.out.println("computeMandelbrot(1)")
  def future = RunnerFactory.getRunner("mandelbrot").submitExecution(config)
  System.out.println("computeMandelbrot(2)")
  def img = future.get().getImage()
  System.out.println("computeMandelbrot(3) img = " + img)
  option.findFirstWithName("/mandelbrotImage").getUIComponent().setImage(img)
  System.out.println("computeMandelbrot(4)")
}

void applyConfig(x, y, d, iter) {
  option.findFirstWithName("/centerX").setValue(x);
  option.findFirstWithName("/centerY").setValue(y);
  option.findFirstWithName("/diameter").setValue(d);
  option.findFirstWithName("/iterations").setValue(iter);
}

void zoom(isZoomIn) {
  def d = option.findFirstWithName("/diameter").getValue();
  def f = option.findFirstWithName("/mandelbrotZoomFactor").getValue();
  option.findFirstWithName("/diameter").setValue(isZoomIn ? d/f : d*f);
  computeMandelbrot(createConfig());
}

void onMouseClicked(event) {
  def button = event.getButton();
  if ((button == MouseEvent.BUTTON1) || (button == MouseEvent.BUTTON3)) {
    def mouseX = event.getX();
    def mouseY = event.getY();
    def centerX = option.findFirstWithName("/centerX").getValue();
    def centerY = option.findFirstWithName("/centerY").getValue();
    def d = option.findFirstWithName("/diameter").getValue();
    def jppfConfig = JPPFConfiguration.getProperties();
    def width = jppfConfig.getInt("image.width", 800);
    def height = jppfConfig.getInt("image.height", 600);
    def minX = centerX - d/2;
    def x = mouseX * d / width + minX;
    def minY = centerY - d/2;
    def y = (height - mouseY - 1) * d / (double) height + minY;
    def f = option.findFirstWithName("/mandelbrotZoomFactor").getValue();

    d = (button == MouseEvent.BUTTON1) ? d/f : d * f;
    def iter = option.findFirstWithName("/iterations").getValue();
    applyConfig(x, y, d, iter);
    computeMandelbrot(createConfig());
  }
}