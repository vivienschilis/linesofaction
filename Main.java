public class Main {
	static final int iter = 1;
	static int depthMax=5;
	static final int msPerMove=1000;

	static long mask_line[] = new long[8+8+15+15];

	static int move_from[] = new int[96*40]; //PROFONDEUR MAX 40
	static int move_to[] = new int[96*40]; //PROFONDEUR MAX 40
	static int move_value[] = new int[96*40]; //PROFONDEUR MAX 40
	static int move = 0;
	
	// Retourne le nombre de groupes sur le plateau
	public static int eat(long a){
		int count=0;
		long cursor=(long)1;
		while(a!=0){
			if((a&cursor)!=0){
				++count;
				long food=cursor;
				long me=0;
				while((a&food)!=0){
					me=me|(a&food);
					a=a&(~food);
					food=(me<<8)|(me>>>8);
					food=food|((food&~mask_line[8])>>>1)|((food&~mask_line[15])<<1);
				}
			}
			cursor=cursor<<1;
		}
		return count;
	}

	public static void initMasks(){
		//Masques horizontaux
		long mask = (long)255;
		for(int i=0;i<8;i++){
			mask_line[i] = mask;
			mask = mask<<8;
		}
		//Masques verticaux
		mask = 0;
		for(int i=0;i<64;i+=8) mask = mask | (long)1<<i;
		for(int i=0;i<8;i++){
			mask_line[8+i] = mask;
			mask = mask<<1;
		}
		//Masques diago desc
		for(int elem=0;elem<15;elem++){
			mask = 0;
			long w = 0;
			int p = (elem<8)?(7-elem)*8:(elem-7);
			w = (long)1<<p;
			int size = (elem<8)?(elem+1):(15-elem);

			for(int i=0;i<size;i++){
				mask = mask | w;
				w = w<<9;
			}
			mask_line[16+elem] = mask;
		}
		//Masques diago asc
		for(int elem=0;elem<15;elem++){
			mask = 0;
			long w = 0;
			int p = (elem<8)?(8-elem)*8-1:14-elem;
			w = (long)1<<p;
			int size = (elem<8)?(elem+1):(15-elem);

			for(int i=0;i<size;i++){
				mask = mask | w;
				w = w<<7;
			}
			mask_line[31+elem] = mask;
		}
	}

	public static void main(String args[]){
		initMasks();
		long inita = initBin();
		long initb = initBin();
		initb = rotateBin(initb);
		long a,b,newa,newb;
		a=0;b=0;
		int i=0;
		System.out.println("Start");
		long debut = System.currentTimeMillis();
		for(int j=0;j<iter;j++){
			a = inita;
			b = initb;
			i = 0;
			while( evaluate(a,b)<1000 && evaluate(b,a)<1000 ){
				if((i&1)==0) playIterativeDeepening(a,b);
				else playMinimax(a,b);
				newa = a & ~( (long)1<<move_from[0] );
				newa = newa | (long)1<<move_to[0];
				newb = b & ~( (long)1<<move_to[0] );
				a=newb;
				b=newa;
				if((i&1)==0){
					displayGame(b,a);
				}else{
					displayGame(a,b);
				}
				i++;
			}
		}
		long fin = System.currentTimeMillis();
		System.out.println("Partie en " + i + " coups");
		System.out.println("Temps de calcul pour " + iter + " itérations : " + (fin-debut) + " ms");
	}

	public static void playRandom(long a,long b){
		move = 0;
		findMoves(a,b);
		int u = (int) (Math.random()*move);
		move_from[0] = move_from[u];
		move_to[0] = move_to[u];
	}

	public static void playFirstMove(long a,long b){
		move = 0;
		findMoves(a,b);
	}
	
	// Fonction heuristique
	public static  int evaluate(long a,long b){
		int qa = eat(a);
		int qb = eat(b);
		if( qa==1 ) return 1000;
		if( qb==1 ) return -1000;
		return 10*(qb-qa) + 2*(2*center(a) - center(b));
	}
	
	public static int maxValue(long a,long b,int alpha,int beta,int depth){
		long newa,newb;
		int value;
		int firstMove,lastMove;
		value = evaluate(a,b);
		if( value>999 || value<-999 ) return value;
		if(depth>=depthMax) return value;
		firstMove=move;
		findMoves(a,b);
		lastMove=move;
		for(int i=firstMove;i<lastMove;i++){
			newa = ( a & ~( (long)1<<move_from[i] )) | (long)1<<move_to[i];
			newb = b & ~( (long)1<<move_to[i] );
			value = minValue(newa,newb,alpha,beta,(depth+1));
			if(value>alpha) alpha=value;
			if(alpha>=beta){
				move = firstMove;
				return beta;
			}
		}
		move = firstMove;
		return alpha;
	}
	
	public static int minValue(long a,long b,int alpha,int beta,int depth){
		long newa,newb;
		int value;
		int firstMove,lastMove;
		value = evaluate(a,b);
		if( value>999 || value<-999 ) return value;
		if(depth>=depthMax) return value;
		firstMove=move;
		findMoves(b,a);
		lastMove=move;
		for(int i=firstMove;i<lastMove;i++){
			newb = ( b & ~( (long)1<<move_from[i] )) | (long)1<<move_to[i];
			newa = a & ~( (long)1<<move_to[i] );
			value = maxValue(newa,newb,alpha,beta,(depth+1));
			if(value<beta) beta=value;
			if(alpha>=beta){
				move = firstMove;
				return beta;
			}
		}
		move = firstMove;
		return beta;
	}
	
	public static int playMinimax(long a,long b){
		move = 0;
		int max = -3000;
		int u = 0;
		int value = 0;
		long newa,newb;
		int firstMove,lastMove;
		firstMove=move;
		findMoves(a,b);
		lastMove=move;
		for(int i=firstMove;i<lastMove && max<1000;i++){
			newa = ( a & ~( (long)1<<move_from[i] ) ) | (long)1<<move_to[i];
			newb = b & ~( (long)1<<move_to[i] );
			value = minValue(newa,newb,-2000,2000,1);
			if( value > max ){
				max = value;
				u = i;
			}
		}
		move_from[0] = move_from[u];
		move_to[0] = move_to[u];
		return max;
	}
	
	public static void playIterativeDeepening(long a,long b){
		depthMax=0;
		int value;
		long start=System.currentTimeMillis();
		do{
			++depthMax;
			value=playMinimax(a,b);
		}while((System.currentTimeMillis()-start)<=msPerMove && value<1000);
		System.out.println("Depth " + depthMax);
	}

	public static void playBestMove(long a,long b){
		move = 0;
		findMoves(a,b);
		int max = Integer.MIN_VALUE;
		int u = 0;
		int value = 0;
		long newa,newb;
		for(int i = 0;i<move;i++){
			newa = a & ~( (long)1<<move_from[i] );
			newa = newa | (long)1<<move_to[i];
			newb = b & ~( (long)1<<move_to[i] );
			value = evaluate(newa,newb);
			if( value > max ){
				max = value;
				u = i;
			}
		}
		move_from[0] = move_from[u];
		move_to[0] = move_to[u];
	}
	
	public static  int center(long a){
		int bits1;
		int bits2;
		int bits3;
		int bits4;
		bits1 = ((int)(a>>>27)&1) + ((int)(a>>>28)&1) + ((int)(a>>>35)&1) + ((int)(a>>>36)&1);
		bits2 = ((int)(a>>>18)&1) + ((int)(a>>>19)&1) + ((int)(a>>>20)&1) + ((int)(a>>>21)&1)
		+ ((int)(a>>>26)&1) + ((int)(a>>>29)&1)
		+ ((int)(a>>>34)&1) + ((int)(a>>>37)&1)
		+ ((int)(a>>>42)&1) + ((int)(a>>>43)&1) + ((int)(a>>>44)&1) + ((int)(a>>>45)&1);
		bits3 = ((int)(a>>>9)&1) + ((int)(a>>>10)&1) + ((int)(a>>>11)&1) + ((int)(a>>>12)&1) + ((int)(a>>>13)&1) + ((int)(a>>>14)&1)
		+ ((int)(a>>>17)&1) + ((int)(a>>>22)&1)
		+ ((int)(a>>>25)&1) + ((int)(a>>>30)&1)
		+ ((int)(a>>>33)&1) + ((int)(a>>>38)&1)
		+ ((int)(a>>>41)&1) + ((int)(a>>>42)&1) + ((int)(a>>>43)&1) + ((int)(a>>>44)&1) + ((int)(a>>>45)&1) + ((int)(a>>>46)&1);
		bits4 = (int)(a&1) + ((int)(a>>>1)&1) + ((int)(a>>>2)&1) + ((int)(a>>>3)&1) + ((int)(a>>>4)&1) + ((int)(a>>>5)&1) + ((int)(a>>>6)&1) + ((int)(a>>>7)&1)
		+ ((int)(a>>>8)&1) + ((int)(a>>>17)&1)
		+ ((int)(a>>>16)&1) + ((int)(a>>>23)&1)
		+ ((int)(a>>>24)&1) + ((int)(a>>>31)&1)
		+ ((int)(a>>>32)&1) + ((int)(a>>>39)&1)
		+ ((int)(a>>>40)&1) + ((int)(a>>>47)&1)
		+ ((int)(a>>>48)&1) + ((int)(a>>>55)&1)
		+ ((int)(a>>>56)&1) + ((int)(a>>>57)&1) + ((int)(a>>>58)&1) + ((int)(a>>>59)&1) + ((int)(a>>>60)&1) + ((int)(a>>>61)&1) + ((int)(a>>>62)&1) + ((int)(a>>>63)&1);
		return 10*bits1+5*bits2+bits3-10*bits4;
	}

	public static  void findMoves( long a, long b ){
		long h,la,mask;
		int bits,elem, i, p2, p3,p,size;

		// Déplacement Horizontal
		for(elem=0;elem<8;++elem){
			mask = mask_line[elem];
			la = ( ( mask & (a) ) );
			if( la!=0 ){
				bits = 0;
				h = (la | (mask & b))>>>((elem<<3));
				bits+=(int)h & 1; h = h>>>1;
				bits+=(int)h & 1; h = h>>>1;
				bits+=(int)h & 1; h = h>>>1;
				bits+=(int)h & 1; h = h>>>1;
				bits+=(int)h & 1; h = h>>>1;
				bits+=(int)h & 1; h = h>>>1;
				bits+=(int)h & 1; h = h>>>1;
				bits+=(int)h & 1;
				for(i=0;i<8;++i){
					p2 = (elem<<3) + i;
					if( (la & (long)1<<(p2)) != 0 ){
						// Sens 1
						if( i+bits<8 ){
							p3 = p2+bits;
							if( (la & (long)1<<(p3))==0 ){
									if(bits<3 || ((la<<(64-p3))>>>(65+p2-p3))==0){
										move_from[move]=p2; move_to[move]=p3;
										move++;
									}
							}
						}
						// Sens 2
						if( i-bits>=0 ){
							p3 = p2-bits;
							if( (la & (long)1<<(p3))==0 ){
									if(bits<3 || ((la<<(64-p2))>>>(65+p3-p2))==0){
										move_from[move]=p2; move_to[move]=p3;
										move++;
									}
							}
						}
					}
				}
			}
			
			// Déplacement Vertical
			mask = mask_line[8+elem];
			la = ( ( mask & (a) ) );
			if( la!=0 ){
				bits = 0;
				h = (la | (mask & b))>>>(elem);
				bits+=(int)h & 1; h = h>>>8;
				bits+=(int)h & 1; h = h>>>8;
				bits+=(int)h & 1; h = h>>>8;
				bits+=(int)h & 1; h = h>>>8;
				bits+=(int)h & 1; h = h>>>8;
				bits+=(int)h & 1; h = h>>>8;
				bits+=(int)h & 1; h = h>>>8;
				bits+=(int)h & 1;

				for( i=0;i<8;++i){
					p2 = elem + (i<<3);
					if( (la & (long)1<<(p2)) != 0 ){
						//Sens 1
						if( i+bits<8 ){
							p3 = p2+ (bits<<3);
							if( (la & (long)1<<(p3))==0 ){
								if( bits<3 || ((la<<(64-p3))>>>(72+p2-p3))==0){
									move_from[move]=p2; move_to[move]=p3;
									++move;
								}
							}
						}
						//Sens 2
						if( i-bits>=0 ){
							p3 = p2 - (bits<<3);
							if( (la & (long)1<<(p3))==0 ){
									if( bits<3 || ((la<<(64-p2))>>>(72+p3-p2))==0){
										move_from[move]=p2; move_to[move]=p3;
										++move;
									}
							}
						}
					}							
				}
			}
		}				

		// Déplacement diagonale descendante
		for( elem=0;elem<15;elem++){
			mask = mask_line[16+elem];
			p = (elem<8)?(7-elem)<<3:(elem-7);
			size = (elem<8)?(elem+1):(15-elem);
			la = ( ( mask & (a) ) );
			if( la!=0 ){
				bits = 0;
				h = (la | (mask & b))>>>(p);
				bits+=(int)h & 1; h = h>>>9;
				bits+=(int)h & 1; h = h>>>9;
				bits+=(int)h & 1; h = h>>>9;						
				bits+=(int)h & 1; h = h>>>9;
				bits+=(int)h & 1; h = h>>>9;
				bits+=(int)h & 1; h = h>>>9;
				bits+=(int)h & 1; h = h>>>9;
				bits+=(int)h & 1;
				for( i=0;i<size;i++){
					p2 = p+i*9;
					if( (la & (long)1<<(p2)) != 0 ){
						//Sens 1
						if( i+bits<size ){
							p3 = p+(i+bits)*9;
							if( (la & (long)1<<(p3))==0 ){
									if(bits<3 || ((la<<(64-p3))>>>(73+p2-p3))==0){
										move_from[move]=p2; move_to[move]=p3;
										++move;
									}
							}
						}
		
						//Sens 2
						if( i-bits>=0 ){
							p3 = p+(i-bits)*9;
							if( (la & (long)1<<(p3))==0 ){
									if(bits<3 || ((la<<(64-p2))>>>(73+p3-p2))==0){
										move_from[move]=p2; move_to[move]=p3;
										++move;
									}
							}
						}
					}							
				}
			}						

			// Déplacement diagonale ascendante
			mask = mask_line[31+elem];
			p = (elem<8)?((8-elem)<<3)-1:14-elem;

			la = ( ( mask & (a) ) );
			if( la!=0 ){
				bits = 0;
				h = (la | (mask & b))>>>(p);
				bits+=(int)h & 1; h = h>>>7;
				bits+=(int)h & 1; h = h>>>7;
				bits+=(int)h & 1; h = h>>>7;
				bits+=(int)h & 1; h = h>>>7;
				bits+=(int)h & 1; h = h>>>7;
				bits+=(int)h & 1; h = h>>>7;
				bits+=(int)h & 1; h = h>>>7;
				bits+=(int)h & 1;
				for( i=0;i<size;i++){
					p2 = p+i*7;
					if( (la & (long)1<<(p2)) != 0 ){
						//Sens 1
						if( i+bits<size ){
							p3 = p+(i+bits)*7;
							if( (la & (long)1<<(p3))==0 ){
									if(bits<3 || ((la<<(64-p3))>>>(71+p2-p3))==0){
										move_from[move]=p2; move_to[move]=p3;
										++move;
									}
							}
						}
		
						//Sens 2
						if( i-bits>=0 ){
							p3 = p+(i-bits)*7;
							if( (la & (long)1<<(p3))==0 ){
									if(bits<3 || ((la<<(64-p2))>>>(71+p3-p2))==0){
										move_from[move]=p2; move_to[move]=p3;
										++move;
									}
							}
						}
					}
				}							
			}
		}
	}

	public static void displayGame(long a,long b){
		long x = 1;
		System.out.println("----------Game----------");
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
				if((a & x)!=0) System.out.print("[x]");
				else if((b & x)!=0) System.out.print("[o]");
				else System.out.print("[ ]");
				x = x<<1;
			}
			System.out.println();
		}
		System.out.println("------------------------");
	}

	public static long rotateBin(long a){
		long b = 0;
		long y = 1;
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
				long p = (((a & y)!=0)?1:0);
				b = b | ( p<< ( i*8+j ) );
				y = y<<8;
				if(y==0) y = 2<<i;
			}
		}
		return b;
	}

	public static void displayBin(long a){
		long x = 1;
		System.out.println("--Long--");
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
				System.out.print( ((a & x)!=0)?1:0 );
				x = x<<1;
			}
			System.out.println();
		}
		System.out.println("--------");
	}

	public static long initBin(){
		return (long)126 + ((long)126<<56);
	}

}
