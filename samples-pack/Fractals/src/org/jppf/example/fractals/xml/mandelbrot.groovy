MandelbrotConfiguration createConfig() {
  def x = option.findFirstWithName("/centerX").getValue();
  def y = option.findFirstWithName("/centerY").getValue();
  def d = option.findFirstWithName("/diameter").getValue();
  def iter = option.findFirstWithName("/iterations").getValue();
  return new MandelbrotConfiguration(x, y, d, iter.intValue());
}

void computeMandelbrot(config) {
  def future = RunnerFactory.getRunner("mandelbrot").submitExecution(config)
  def img = future.get().getImage()
  option.findFirstWithName("/mandelbrotImage").getUIComponent().setImage(img)
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