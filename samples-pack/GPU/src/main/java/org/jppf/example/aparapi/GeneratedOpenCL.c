typedef struct This_s{
   __global float *kernelMatrixA;
   int size;
   __global float *kernelMatrixB;
   __global float *kernelResults;
   int passid;
}This;
int get_pass_id(This *this){
   return this->passid;
}
void test_AparapiTask$MatrixKernel__multiply(This *this, int rowA, int columnB){
   float sum = 0.0f;
   for (int i = 0; i<this->size; i++){
      sum = sum + (this->kernelMatrixA[((rowA * this->size) + i)] * this->kernelMatrixB[((i * this->size) + columnB)]);
   }
   this->kernelResults[(columnB * this->size) + rowA]  = sum;
   return;
}
__kernel void run(
   __global float *kernelMatrixA, 
   int size, 
   __global float *kernelMatrixB, 
   __global float *kernelResults, 
   int passid
){
   This thisStruct;
   This* this=&thisStruct;
   this->kernelMatrixA = kernelMatrixA;
   this->size = size;
   this->kernelMatrixB = kernelMatrixB;
   this->kernelResults = kernelResults;
   this->passid = passid;
   {
      int rowA = get_global_id(0);
      for (int colB = 0; colB<this->size; colB++){
         test_AparapiTask$MatrixKernel__multiply(this, rowA, colB);
      }
      return;
   }
}