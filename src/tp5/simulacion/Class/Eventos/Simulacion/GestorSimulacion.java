/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp5.simulacion.Class.Eventos.Simulacion;

import Eventos.IngresoPuerto;
import Eventos.LlegadaBuque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tp5.simulacion.Class.Objetos.Buque;
import tp5.simulacion.Class.Objetos.Tanque;
import distribuciones.*;
/**
 *
 * @author aleex
 */
public class GestorSimulacion {
    //Para el caso de las celdas que van a quedar vacias, se coloca el valor -1 por defecto
    private double reloj;
    private VectorEstado vectorEstadoActual;
    //Random para manejar la generacion de random 
    private Random rnd;
    private int cola;
    //Eventos 
    private LlegadaBuque llegadaBuque;
    private IngresoPuerto ingresoPuerto;
    
    private Buque[] buques;//Vector para manejar los buques
    //Objetos Permanentes
    private Tanque tanque1;
    private Tanque tanque2;
    private Tanque tanque3;
    private Tanque tanque4;
    private Tanque tanque5;
    //Vector para poder recorrer los tanques(servidores) (permanentes)
    private Tanque[] tanques;
    //Atributo para alamacenar los vectores estados que se van a mostrar en la tabla
    private ObservableList<VectorEstadoView> vectoresEstados;
    //Atributos para saber desde y hasta que fila mostrar
    private double horaDesdeVER;
    private double horaHastaVER;
    //Atributo que se utiliza para cortar la simulacion en una hora determinada
    private double horaHasta;
    //Atributo que se utiliza para contar las filas, para poder mostrar luego
    
    public GestorSimulacion(double desde , double hasta, double horaHasta) {
        this.rnd = new Random();
        this.vectoresEstados = FXCollections.observableArrayList();
        this.horaHasta = horaHasta;
        this.horaDesdeVER = desde;
        this.horaHastaVER = hasta;
        this.reloj = 0;
        this.cola = 0;
        this.llegadaBuque = new LlegadaBuque();
        this.llegadaBuque.setProximaLllegada(0);//Condicion inicial;
        this.ingresoPuerto = new IngresoPuerto();
        //Inicializo los tanques con los valores por defecto del problema
        this.tanque1 = new Tanque(70000,"L");
        this.tanque2 = new Tanque(70000,"L");
        this.tanque3 = new Tanque(0,"D");
        this.tanque3.setFinDescarga(8);
        this.tanque4 = new Tanque(45000, "C");
        this.tanque4.setFinCarga(12);
        this.tanque5 = new Tanque(25000, "C");
        this.tanque5.setFinCarga(3.5);
        //ArrayList de tanque para generar el evento que sigue
        this.tanques = new Tanque[5];
        this.tanques[0] = tanque1;
        this.tanques[1] = tanque2;
        this.tanques[2] = tanque3;
        this.tanques[3] = tanque4;
        this.tanques[4] = tanque5;
        this.buques = new Buque[5];
    }
    
    
    private void simularEventoFinCarga(double finCarga){
        this.reloj = this.vectorEstadoActual.getReloj() + finCarga;
        
    }
    //Metodo separado, ya que en todos los casos son iguales
    private void simularLlegada(){
        double rndLlegada = this.rnd.nextDouble();
        double tiempoLlegada = this.llegadaBuque.generarTiempoLlegada(rndLlegada);
        double proximaLlegada = this.reloj + tiempoLlegada;
        this.llegadaBuque.setProximaLllegada(proximaLlegada);
        this.llegadaBuque.setRndLLegadaBuque(rndLlegada);
        this.llegadaBuque.setTiempoLLegadaBuque(tiempoLlegada);
    }
    //Metodo separado, ya que en todos los casos son iguales
    private void simularIngresoPuerto(){
        double rndContenidoBuque = this.rnd.nextDouble();
        double cargaBuque = this.ingresoPuerto.generarCarga(rndContenidoBuque);
        this.ingresoPuerto.setCargaActual(cargaBuque);
        this.ingresoPuerto.setRndContenido(rndContenidoBuque);
    }
    
    private void simularEventoLlegadaBuque() {
        //Seteo del reloj
        this.reloj = this.vectorEstadoActual.getReloj() + this.vectorEstadoActual.getProximaLllegada();
        
        //EVENTO LLEGADA DEL Proximo buque BUQUE
        this.simularLlegada();
        
        //EVENTO Ingreso Buque ; PRimero me fijo si hay tanque libre
        Tanque tanqueLibre = this.getTanqueLibre();
        if (tanqueLibre == null) {
            this.cola += 1;
            this.vectorEstadoActual.setCola(this.cola);
         }
        else {
            this.simularIngresoPuerto();
            Buque buqueNuevo = new Buque();
            buqueNuevo.ponerSiendoAtendido();
            buqueNuevo.setCargaActual(this.ingresoPuerto.getCargaActual());
            double finCarga = this.generarFinCarga(buqueNuevo.getCargaActual());
            
            //coloca el buque en la casilla del tanque, osea tanque1 con buque1 ...
            int index = this.getIndexTanque(tanqueLibre);
            this.buques[index] = buqueNuevo;
            tanqueLibre.setFinCarga(finCarga);
            tanqueLibre.setEstado("C");
            tanqueLibre.setBuqueEnAtencion(buqueNuevo);
            
            if(tanqueLibre.getCapacidadLibre() < tanqueLibre.getBuqueEnAtencion().getCargaActual()){
                //Forma de calcular la descarga, 70000/4000 = 17.5
                tanqueLibre.generarProximaInterrupcion(this.reloj);
            }
        }
        this.actualizarVectorEstadoActual();
    }
    
    public void actualizarVectorEstadoActual(){
        //LA idea es que en este metodo se actualize todos los atributos del vector estado, 
        //con los valores actuales de los objetos
        Buque b1 = this.tanque1.getBuqueEnAtencion();
        Buque b2 = this.tanque2.getBuqueEnAtencion();
        Buque b3 = this.tanque3.getBuqueEnAtencion();
        Buque b4 = this.tanque4.getBuqueEnAtencion();
        Buque b5 = this.tanque5.getBuqueEnAtencion();
        this.vectorEstadoActual = new VectorEstado(this.reloj, this.llegadaBuque.getRndLLegadaBuque(), 
               this.llegadaBuque.getTiempoLLegadaBuque(),this.llegadaBuque.getProximaLllegada(),
               this.ingresoPuerto.getRndContenido() , this.ingresoPuerto.getCargaActual(), 
               this.tanque1.getCapacidadLibre(), this.tanque1.getEstado(), this.tanque1.getProximaInterrupcion(), this.tanque1.getFinDescarga(),
               this.tanque2.getCapacidadLibre(), this.tanque2.getEstado(), this.tanque2.getProximaInterrupcion(), this.tanque1.getFinDescarga(),
               this.tanque3.getCapacidadLibre(), this.tanque3.getEstado(), this.tanque3.getProximaInterrupcion(), this.tanque1.getFinDescarga(),
               this.tanque4.getCapacidadLibre(), this.tanque4.getEstado(), this.tanque4.getProximaInterrupcion(), this.tanque1.getFinDescarga(),
               this.tanque5.getCapacidadLibre(), this.tanque5.getEstado(), this.tanque5.getProximaInterrupcion(), this.tanque1.getFinDescarga(),
               b1.getCargaActual() , b1.getEstado(), b1.getTiempoRemanenteCarga(), "tanque 1",
               b2.getCargaActual() , b2.getEstado(), b2.getTiempoRemanenteCarga(), "tanque 2",
               b3.getCargaActual() , b3.getEstado(), b3.getTiempoRemanenteCarga(), "tanque 3",
               b4.getCargaActual() , b4.getEstado(), b4.getTiempoRemanenteCarga(), "tanque 4",
               b5.getCargaActual() , b5.getEstado(), b5.getTiempoRemanenteCarga(), "tanque 5",
               this.cola
        );
    }
    
    public String proximoEvento(){
        //Si este metodo funciona bien, deberia seleciionar el evento proximo
        double minimaHora = 0;
        String evento;
        for(int i = 0 ; i < this.tanques.length ; i++){
            if(minimaHora < this.tanques[i].getFinCarga()){
                minimaHora = this.tanques[i].getFinCarga();
                evento = "FinCarga";
            }
            if(minimaHora < this.tanques[i].getFinDescarga()){
                minimaHora = this.tanques[i].getFinDescarga();
                evento = "FinDescarga";
            }
        }
        if(this.llegadaBuque.getProximaLllegada() < minimaHora){
            return "LlegadaBuque";
        }
        return null;
    }
    
    /*public Object buscarProximoEvento(){
        //Metodo que deberia devolver el proximo evento , Object porque puede ser un Tanque o una Llegada,
        //Dentro del tanque esta la hora fin carga y hora fin descarga
        Object o;
        return o;
    }*/
    public ObservableList<VectorEstadoView> simular()
    {
        //Seteo el vector estado Actual en la posicion 0 del reloj
       
        
        while (this.reloj <= this.horaHasta){
            //ACA DEBERIA IR TODA LA LOGICA DE LA SIMULACION
            //ACa se deberia buscar cual es el evento siguiente
            /*FinCarga fc = this.proximoFinCarga();
            if (fc.getFinCarga() < fd.getHoraFinDescarga() && fc.getFinCarga() < this.llegadaBuque.getProxLlegada()){
                
            }*/
          
            //Al final de cada ciclo para almacenar los que se deben mostrar
            //if ( this.filaActual <= this.filaHasta && this.filaActual >= this.filaDesde) {
            //    VectorEstadoView v = new VectorEstadoView(this.vectorEstadoActual);
            //    this.vectoresEstados.add(v);
            //}
        }
        
        return this.vectoresEstados;
    }
    
    public double generarFinCarga(double cargaBuque){
        double aux = 0.5 + cargaBuque / 10000.0;
        aux = Math.round(aux * 100.0) / 100.0;
        return aux;
    }
    
    
    
    private Tanque getTanqueLibre(){
        for (Object tanque: this.tanques){
            Tanque tanqueAux = (Tanque) tanque;
            if (tanqueAux.getEstado() == "L"){
                return tanqueAux;
            }
        }
        return null;
    }
    
    private int getIndexTanque(Tanque t){
        for (int i = 0; i < this.tanques.length ; i++){
            if (this.tanques[i].equals(t)){
                return i;
            }
        }
        return -1;
    }
}
