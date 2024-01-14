//Test 302

program test302

int niz[];
int i;
int minimax;

{
	void main()
	{
//        niz = new int[10];
//        for(i = 0; i < 10; i++)
//            niz[i] = i;
//        minimax = max niz;
//        print(minimax)
	}
}

//////////////////////// TODO ////////////////////////

//////////////// instructions
------------ load 0             // index = 0
------------ load arr           // load array
------------ aload              // load array[index]
------------ load 1             // load index=1
loopAdr:---- dup                // duplicate index
------------ load arr           // load array
------------ arraylength        // load array length
------------ lt                 // index < array.length
------------ jmpFalse end       // if false jump to end
------------ dup                // duplicate index
------------ dup_x2             // move index 2 places up
------------ pop                // remove index
------------ load arr           // load array
------------ dup_x1             // last element move 1 place up
------------ pop                // remove index
------------ aload              // load array[index]
------------ dup2               // duplicate last 2 elements
------------ lt                 // array[index] < array[index+1]
------------ jmpFalse newMax    // if false jump to newMax
------------ dup_x1             // move index 1 place up
------------ pop                // remove index
############ fixUp changeMax    // fix jump address
newMax:----- pop                // remove index
------------ dup_x1             // move index 1 place up
------------ pop                // remove index
------------ load 1             // load index=1
------------ add                // index++
------------ jmp loopAdr        // jump to loopAdr
############ fixUp end          // fix jump address
end:-------- pop                // remove index

arr = 55
arr = { 7, 8, 9}
//////////////// stack
------------ 8 // arr[1]
------------ 2
------------ 2
------------ 3





