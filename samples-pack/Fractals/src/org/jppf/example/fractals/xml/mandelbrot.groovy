MandelbrotConfiguration createConfig() {
  def x = option.findFirstWithName("/centerX").getValue();
  def y = option.findFirstWithName("/centerY").getValue();
  def d = option.findFirstWithName("/diameter").getValue();
  def iter = option.findFirstWithName("/iterations").getValue();
  return new MandelbrotConfiguration(x, y, d, iter.intValue());
}

void computeMandelbrot(config) {
  RunnerFactory.getRunner("mandelbrot").submitExecution(config);
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

