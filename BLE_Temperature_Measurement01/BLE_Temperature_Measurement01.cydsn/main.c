/*******************************************************************************
* File Name: main.c
*
* Version: 1.0
*
* Description:
*  Temperature measurement example project that simulates thermometer readings
*  and sends it over BLE Health Thermometer service.
*
* Note:
*
* Hardware Dependency:
*  CY8CKIT-042 BLE
* 
********************************************************************************
* Copyright 2016, Cypress Semiconductor Corporation.  All rights reserved.
* You may use this file only in accordance with the license, terms, conditions,
* disclaimers, and limitations in the end user license agreement accompanying
* the software package with which this file was provided.
*******************************************************************************/

#include "common.h"
uint8 busStatus = CYBLE_STACK_STATE_FREE;           /* Status of stack queue */

volatile uint32 mainTimer = 0;

const double TEMP_MAX = 120;
const double TEMP_MIN = 55;

int currentTemp = 60;
int desiredTemp = 60;

//Updates the GATT database with the current temperature
void updateTemperature() {
    if(CyBle_GetState() != CYBLE_STATE_CONNECTED)
        return;
    
    CYBLE_GATTS_HANDLE_VALUE_NTF_T tempHandle;
    
    tempHandle.attrHandle = currentTemp;
    tempHandle.value.len = 4;
    CyBle_GattsWriteAttributeValue(&tempHandle, 0, &cyBle_connHandle, CYBLE_GATT_DB_LOCALLY_INITIATED);
}

void controlTemperature(){
    while(desiredTemp != currentTemp) {
        
        if(desiredTemp > currentTemp) {
            Advertising_LED_Write(LED_OFF);
            LowPower_LED_Write(LED_ON);
            currentTemp++;
            Pin_1_Write(0);
        }
        else if(desiredTemp < currentTemp) {
            Advertising_LED_Write(LED_OFF);
            LowPower_LED_Write(LED_ON);
            currentTemp--;
            Pin_1_Write(1);
        }
        if(desiredTemp == currentTemp) {
            Advertising_LED_Write(LED_ON);
            LowPower_LED_Write(LED_OFF);
        }
    }
    updateTemperature();
}

//Set the temp from the App
void setTemperature(int temp) {
    if(temp > TEMP_MAX) {
        temp = TEMP_MAX;
    }
    if(temp < TEMP_MIN) {
        temp = TEMP_MIN;
    }
    
    desiredTemp = temp;
    controlTemperature();
}

/*******************************************************************************
* Function Name: AppCallBack()
********************************************************************************
*
* Summary:
*   This is an event callback function to receive events from the BLE Component.
*
* Parameters:
*  event - the event code
*  *eventParam - the event parameters
*
*******************************************************************************/
void AppCallBack(uint32 event, void* eventParam)
{
    CYBLE_GATTS_WRITE_REQ_PARAM_T *wrReqParam;
    
    switch(event)
    {
        case CYBLE_EVT_STACK_ON:
        case CYBLE_EVT_GAP_DEVICE_DISCONNECTED:
            Disconnect_LED_Write(LED_ON);
            LowPower_LED_Write(LED_OFF);
            CyBle_GappStartAdvertisement(CYBLE_ADVERTISING_FAST);
        break;
            
        case CYBLE_EVT_GATT_CONNECT_IND:
            
            Disconnect_LED_Write(LED_OFF);
            LowPower_LED_Write(LED_ON);
        break;
            
        case CYBLE_EVT_GATTS_WRITE_REQ:
            wrReqParam = (CYBLE_GATTS_WRITE_REQ_PARAM_T *) eventParam;
            
            setTemperature((int8)wrReqParam->handleValPair.value.val[0]);    
            
            CyBle_GattsWriteRsp(cyBle_connHandle);
            
        break;
               
        default: 
            
        break;
    }
}




/*******************************************************************************
* Function Name: LowPowerImplementation()
********************************************************************************
* Summary:
* Implements low power in the project.
*
* Parameters:
* None
*
* Return:
* None
*
* Theory:
* The function tries to enter deep sleep as much as possible - whenever the 
* BLE is idle and the UART transmission/reception is not happening. At all other
* times, the function tries to enter CPU sleep.
*
*******************************************************************************/
static void LowPowerImplementation(void)
{
    CYBLE_LP_MODE_T bleMode;
    uint8 interruptStatus;
    
    /* For advertising and connected states, implement deep sleep 
     * functionality to achieve low power in the system. For more details
     * on the low power implementation, refer to the Low Power Application 
     * Note.
     */
    if((CyBle_GetState() == CYBLE_STATE_ADVERTISING) || 
       (CyBle_GetState() == CYBLE_STATE_CONNECTED))
    {
        /* Request BLE subsystem to enter into Deep-Sleep mode between connection and advertising intervals */
        bleMode = CyBle_EnterLPM(CYBLE_BLESS_DEEPSLEEP);
        /* Disable global interrupts */
        interruptStatus = CyEnterCriticalSection();
        /* When BLE subsystem has been put into Deep-Sleep mode */
        if(bleMode == CYBLE_BLESS_DEEPSLEEP)
        {
            /* And it is still there or ECO is on */
            if((CyBle_GetBleSsState() == CYBLE_BLESS_STATE_ECO_ON) || 
               (CyBle_GetBleSsState() == CYBLE_BLESS_STATE_DEEPSLEEP))
            {
            #if (DEBUG_UART_ENABLED == ENABLED)
                /* Put the CPU into the Deep-Sleep mode when all debug information has been sent */
                
            #else
                CySysPmDeepSleep();
            #endif /* (DEBUG_UART_ENABLED == ENABLED) */
            }
        }
        else /* When BLE subsystem has been put into Sleep mode or is active */
        {
            /* And hardware doesn't finish Tx/Rx opeation - put the CPU into Sleep mode */
            if(CyBle_GetBleSsState() != CYBLE_BLESS_STATE_EVENT_CLOSE)
            {
                CySysPmSleep();
            }
        }
        /* Enable global interrupt */
        CyExitCriticalSection(interruptStatus);
    }
}


/*******************************************************************************
* Function Name: main()
********************************************************************************
* Summary:
*  Main function for the project.
*
* Parameters:
*  None
*
* Return:
*  None
*
* Theory:
*  The function starts BLE and UART components.
*  This function process all BLE events and also implements the low power 
*  functionality.
*
*******************************************************************************/
int main()
{
    CyGlobalIntEnable;  

    Disconnect_LED_Write(LED_ON);
    Advertising_LED_Write(LED_OFF);
    LowPower_LED_Write(LED_OFF);

    
    /* Start CYBLE component and register generic event handler */
    CyBle_Start(AppCallBack);
    /* Register service specific callback functions */
    controlTemperature();
    //currentTemp = I2c_Read();
    /***************************************************************************
    * Main polling loop
    ***************************************************************************/
	while(1) 
    {
        /* To achieve low power in the device */
        //LowPowerImplementation();
        
        /* CyBle_ProcessEvents() allows BLE stack to process pending events */
        CyBle_ProcessEvents();

        //Check temp, turn on/off pin  
        controlTemperature();
	}   
}  


/* [] END OF FILE */

