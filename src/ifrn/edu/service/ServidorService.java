/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ifrn.edu.service;

import ifrn.edu.bean.ChatMensagem;
import ifrn.edu.bean.ChatMensagem.Action;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Monic
 */
public class ServidorService {

    private ServerSocket serversocket;
    private Socket socket;
    private Map<String, ObjectOutputStream> mapOnline = new HashMap<String, ObjectOutputStream>();

    public ServidorService() {
        try {
            serversocket = new ServerSocket(5555);
            System.out.println("Servidor online!");
            while (true) {
                socket = serversocket.accept();
                new Thread(new ListenerSocket(socket)).start();
            }
        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class ListenerSocket implements Runnable {

        private ObjectOutputStream output;
        private ObjectInputStream input;

        public ListenerSocket(Socket socket) {
            try {
                this.output = new ObjectOutputStream(socket.getOutputStream());
                this.input = new ObjectInputStream(socket.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        @Override
        public void run() {
            ChatMensagem mensagem = null;
            try {
                while ((mensagem = (ChatMensagem) input.readObject()) != null) {
                    Action action = mensagem.getAction();
                    if (action.equals(Action.CONNECT)) {
                        boolean isconnect = connect(mensagem, output);
                        if(isconnect){
                            mapOnline.put(mensagem.getName(), output);
                            sendOnline();
                        }
                        
                    } else if (action.equals(Action.DISCONNECT)) {
                        desconecte(mensagem,output);
                        sendOnline();
                        return;
                    } else if (action.equals(Action.SEND_ONE)) {
                        sendOne(mensagem);

                    } else if (action.equals(Action.SEND_ALL)) {
                        sendAll(mensagem);

                    }
                }
            } catch (IOException ex) {
                desconecte(mensagem, output);
                sendOnline();
                System.out.println(mensagem.getName() + " deixou o chat");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private boolean connect(ChatMensagem mensagem, ObjectOutputStream output) {
        if(mapOnline.size() == 0){
            mensagem.setTexto("Sim");
            send(mensagem, output);
            return true;
        }
         
        for(Map.Entry<String, ObjectOutputStream> kv: mapOnline.entrySet()){
            if(kv.getKey().equals(mensagem.getName())){
                mensagem.setTexto("Não");
                send(mensagem, output);
                return false;
            }else{
                mensagem.setTexto("Sim");
                send(mensagem, output);
                return true;
            }
        }
        //Não ocorrerá    
        return false;
    }
    private void desconecte(ChatMensagem mensagem, ObjectOutputStream output){
        mapOnline.remove(mensagem.getName());
        mensagem.setTexto(" Saiu! ");
        mensagem.setAction(Action.SEND_ONE);
        sendAll(mensagem);
        System.out.println("User " + mensagem.getName() + " Saiu do chat");
        
    }
    private void send(ChatMensagem mensagem, ObjectOutputStream output) {
        try {
            output.writeObject(mensagem);
        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendOne(ChatMensagem mensagem) {
        for(Map.Entry<String, ObjectOutputStream> kv : mapOnline.entrySet()){
            if(kv.getKey().equals(mensagem.getNomeReservado())){            
        try {
            kv.getValue().writeObject(mensagem);
        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    private void sendAll(ChatMensagem mensagem){
        for(Map.Entry<String, ObjectOutputStream> kv : mapOnline.entrySet()){
            if(!kv.getKey().equals(mensagem.getName())){
                mensagem.setAction(Action.SEND_ONE);
                try {
                    kv.getValue().writeObject(mensagem);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    private void sendOnline(){
        Set<String> setNames = new HashSet<String>();
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnline.entrySet()){
            setNames.add(kv.getKey());
        }
        ChatMensagem mensagem = new ChatMensagem();
        mensagem.setAction(Action.USERS_ONLINE);
        mensagem.setSetOnline(setNames);
        for(Map.Entry<String, ObjectOutputStream> kv : mapOnline.entrySet()){
            mensagem.setName(kv.getKey());
            try {
                kv.getValue().writeObject(mensagem);
            } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
}
