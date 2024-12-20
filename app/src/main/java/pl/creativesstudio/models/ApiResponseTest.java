package pl.creativesstudio.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    private ApiResponse apiResponse;
    private List<Bus> busList;

    @BeforeEach
    void setUp() {
        apiResponse = new ApiResponse();
        busList = new ArrayList<>();

        Bus bus1 = new Bus();
        bus1.setLines("Bus1");
        bus1.setTime("10:00");
        busList.add(bus1);

        Bus bus2 = new Bus();
        bus2.setLines("Bus2");
        bus2.setTime("11:00");
        busList.add(bus2);
    }

    @Test
    void testGetResultWhenListIsNull() {
        assertNull(apiResponse.getResult(), "Początkowa wartość pola result powinna być null");
    }

    @Test
    void testSetResult() {
        apiResponse.setResult(busList);
        assertNotNull(apiResponse.getResult(), "Wynik nie powinien być null po ustawieniu listy");
        assertEquals(2, apiResponse.getResult().size(), "Lista powinna mieć 2 elementy");
    }

    @Test
    void testGetResultReturnsCorrectList() {
        apiResponse.setResult(busList);
        List<Bus> result = apiResponse.getResult();

        assertNotNull(result, "Wynik nie powinien być null");
        assertEquals(busList, result, "Lista powinna być identyczna z ustawioną listą");
    }

    @Test
    void testSetResultOverwritesPreviousValue() {
        List<Bus> newBusList = new ArrayList<>();
        Bus newBus = new Bus();
        newBus.setLines("Bus3");
        newBus.setTime("12:00");
        newBusList.add(newBus);

        apiResponse.setResult(busList);
        apiResponse.setResult(newBusList);

        assertEquals(1, apiResponse.getResult().size(), "Lista powinna zawierać jeden element po nadpisaniu");
        assertEquals("Bus3", apiResponse.getResult().get(0).getLines(), "Nazwa autobusu powinna być Bus3");
    }
}