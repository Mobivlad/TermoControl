#define F_CPU 14745600UL
#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <avr/eeprom.h>
#include <stdbool.h>
#define SetBit(Port,bit) Port|=(1<<bit)
#define ClrBit(Port,bit) Port&=~(1<<bit)
#define InvBit(Port,bit) Port^=(1<<bit)
uint8_t EEMEM eeprombyte[34]={
	0X00,0X00,0X00,0X00,0X00,0X00,0X00,0X00,0X00,0X00,
	0X00,0X00,0X00,0X00,0X00,0X00,0X00,0X00,0X00,0X00,
	0X00,0X00,0X00,0X00,0X00,0X00,0X00,0X00,0X00,0X00,
	0X00,0X00,0X00,0X00};

#define InitTimer1 TCCR1A=0x00;TCCR1B=0x04; TCNT1H=0x00;TCNT1L=0x00; ICR1H=0x00;ICR1L=0x00;OCR1AH=0x00;OCR1AL=0x00;OCR1BH=0x00;OCR1BL=0x00

#define InitTimer0  TCNT0=0xE0;TCCR0A=0x00; TCCR0B=0x00; TIMSK=0x02; //встановлення таймера0
#define StartTimer0 TCNT0=0xE0;TCCR0A=0x00; TCCR0B=0x05 //запуск таймера0 1:1024
#define EnableUART SetBit(UCSRB,RXEN); SetBit(UCSRB,RXCIE)
#define DisableUART SetBit(UCSRB,RXEN); SetBit(UCSRB,RXCIE)

#define MaxLenghtRecBuf 8					//розмір буфера для даних що приймаються з UART (COM-порт)
unsigned char cmRcBuf0[MaxLenghtRecBuf] ;	//буфер прийому даних
unsigned char cmTrBuf0[MaxLenghtRecBuf] ;	//буфер прийому даних
unsigned char cNumRcByte0,cNumTrByte0;		//передає у обробку кількість прийнятих байт
char cTempUART,d,msec;
unsigned char RcCount,TrCount;				//лічильник переданих/прийнятих даних
bool StartRec=false;						// false/true початок/прийом посилки

uint8_t min;
uint16_t light,tt,sec;
volatile bool j, g, dim, dimming, up, trstim, trsdim, transdim, transstan,timoff;


void delay_100us(uint16_t n) {
	while (n--) {
		_delay_us(90);
	}
}

ISR (INT0_vect)
{
	if (!dim) {
		ClrBit(PORTB,4);
		if (j){
			_delay_us(500);
			SetBit(PORTB,4);
		};
	}
	else{
		d++;
		if (dimming&(d>3)){
			d=0;
			if (up){
				light++;
				if (light>79) up=false;
			}
			else {
				light--;
				if (light<1) up=true;
			};
		};		
		if (j){
			if (tt>light){tt--;};	
			if (tt<light){tt++;};
			_delay_us(300); delay_100us(tt);SetBit(PORTB,4);_delay_ms(1);ClrBit(PORTB,4);
		};
	};
	if (timoff){
		msec++; if (msec>99){msec=0;sec++;InvBit(PORTD,4);};
		};
		
}



#define UPE 2
#define DOR 3
#define FE 4
#define UDRE 5
//#define RXC 7

#define FRAMING_ERROR (1<<FE)
#define PARITY_ERROR (1<<UPE)
#define DATA_OVERRUN (1<<DOR)
#define DATA_REGISTER_EMPTY (1<<UDRE)
//#define RX_COMPLETE (1<<RXC)

ISR(USART_RX_vect){
	cTempUART=UDR;
//	if  (UCSRA&((1<<FE))) return;			//FE0-помилка кадра, OVR - переповнення даних
if (UCSRA & (FRAMING_ERROR | PARITY_ERROR | DATA_OVERRUN)) {return;};
	if (!StartRec){						//якщо це перший байт, то починаємо прийом
		StartRec=true;
		RcCount=0;
		cmRcBuf0[RcCount]=cTempUART;
		RcCount++;
		StartTimer0;
	}
	else{								//продовжуємо прийом
		if (RcCount<MaxLenghtRecBuf){	//якщо ще не кінець буфера
			cmRcBuf0[RcCount]=cTempUART;
			RcCount++;
			}else{						//буфер переповнений
			cmRcBuf0[MaxLenghtRecBuf-1]=cTempUART;
		}
		TCNT0=0xC0;						//перезапуск таймера
	}
}

void StartTrans0(void){  //функція передачі даних
	TrCount=0;
	while (TrCount<cNumTrByte0)
	{
		while ((UCSRA & DATA_REGISTER_EMPTY)==0);
		UDR=cmTrBuf0[TrCount];
		TrCount++;
	}
}//end  void StartTrans1()

ISR(TIMER0_OVF_vect){
unsigned char reg, num;
	if (StartRec){
		StartRec=false;			//посилка прийнята
		cNumRcByte0=RcCount;	//кількість прийнятих байт
		TCCR0B=0x00;			//зупиняємо таймер ноль
		reg=cmRcBuf0[1];
		num=cmRcBuf0[0];


			switch (reg){
			case 1: {if(num==1){j=true;SetBit(PORTD,4);}else{j=false;ClrBit(PORTD,4);}; transstan=true; break;};
			case 2: {dim=num; trsdim=true; eeprom_write_byte(0,dim);break;};
			case 3: {light=num;break;};
			case 4: {min=num;eeprom_write_byte(1,min);break;};
			case 5: {timoff=num; trstim=true;break;};
	
			};//switch
	};
}

void StartUART0(void){
	UBRRH=0x00;
	UBRRL=0x07;	//14.7456 MHz --   19200 br 0x2F  /  9600 br  0x5F / 115200 UBRRL=0x07;
				// 8.0000 MHz --   19200 br 0x19  /  9600 br  0x33 / 38400 br 0x0C
	UCSRA=0x00;
	UCSRB=0x98;	//0x98
	UCSRC=0x0E; // 0x0E-2 stop bits ; 0x06-1 stop bit

}//end void StartUART0()

ISR (INT1_vect)
{
	if ((TCNT1>4093)&(TCNT1<20000)&j&g){// Виключення з кнопки
		j=false;
		sec=0;
		timoff=false;
		ClrBit(PORTD,4);
		if (PIND&(1<<5)) transstan=true;
		};
	
	TCNT1=0;
	g=true;
	if (dimming){dimming=false;TCCR1B=0x04;transdim=true;};
}


int main(void)
{
 char k;
	j=false;
	g=false;
	k=0;
	sec=0;
	msec=0;
	light=65;
	transdim=false;
	trsdim=false;
	transstan=false;
	trstim=false;
	timoff=false;
	PORTB=0x00;
	DDRB=0b00010000;
	PORTD=0x00;
	DDRD=0b00010000;
	dim=eeprom_read_byte(0);
	min=eeprom_read_byte(1); if (min<1) {min=1;};
	StartUART0();
	InitTimer1;
	InitTimer0;
	MCUCR=0x05;
	GIMSK=0xC0;
	GIFR=0xC0;
	sei();
	while(1)
	{

		if ((PIND&(1<<5))&&(k<5)){
			k++;
			TCNT1=0;TCCR1B=0x00;
			_delay_ms(200);
			if(k==1){ cNumTrByte0=2;	cmTrBuf0[0]=1;	cmTrBuf0[1]=j;	 StartTrans0();};
			if(k==2){ cNumTrByte0=2;	cmTrBuf0[0]=2;	cmTrBuf0[1]=dim; StartTrans0();};
			if(k==3){ cNumTrByte0=2;	cmTrBuf0[0]=3;	cmTrBuf0[1]=light; StartTrans0();};
			if(k==4){ cNumTrByte0=2;	cmTrBuf0[0]=4;	cmTrBuf0[1]=min; StartTrans0();};
			if(k==5){ cNumTrByte0=2;	cmTrBuf0[0]=5;	cmTrBuf0[1]=timoff; StartTrans0();};
			TCCR1B=0x04;	
			};
		if (!(PIND&(1<<5))){
			k=0;};
			
//---------------------------------------------
		if ((TCNT1>4096)&!j){// Включення з кнопки
			SetBit(PORTD,4);
			j=true;
			g=false;
			if (PIND&(1<<5)) transstan=true;
			TCNT1=0;
			};
//----------------------------------------------		
		if (timoff&(sec>min*60)){// Таймер виключення
			j=false;
			sec=0;
			timoff=false;
			ClrBit(PORTD,4);
			transstan=true;
			trstim=true;
			};
//----------------------------------------------		
		if ((TCNT1>20000)&j&dim){TCCR1B=0x00; dimming=true;};
		if ((TCNT1>20000)&j&!dim){timoff=true;};	
		if ((PIND&(1<<5))&&transdim) {transdim=false;  cNumTrByte0=2;	cmTrBuf0[0]=3;	cmTrBuf0[1]=light;	_delay_ms(100); StartTrans0(); _delay_ms(100);};
		if ((PIND&(1<<5))&&trsdim)   {trsdim=false;	   cNumTrByte0=2;	cmTrBuf0[0]=2;	cmTrBuf0[1]=dim;	_delay_ms(100); StartTrans0(); _delay_ms(100);};	
		if ((PIND&(1<<5))&&transstan){transstan=false; cNumTrByte0=2;	cmTrBuf0[0]=1; 	cmTrBuf0[1]=j;		_delay_ms(100); StartTrans0(); _delay_ms(100);};
		if ((PIND&(1<<5))&&trstim)	 {trstim=false;	   cNumTrByte0=2;	cmTrBuf0[0]=5;	cmTrBuf0[1]=timoff;	_delay_ms(100); StartTrans0(); _delay_ms(100);};
		
	}
}