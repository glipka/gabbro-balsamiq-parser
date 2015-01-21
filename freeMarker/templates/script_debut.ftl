 <script>
 $(function () {
 // Détection du support
if(typeof localStorage!='undefined') {
    alert("localStorage est supporté");
	var tab=new Array;
   tab[0]="Lundi";
   tab[1]="Mardi";
   tab[2]="Mercredi";
   tab[3]="Jeudi";
   tab[4]="Vendredi";
   tab[5]="Samedi";
   tab[6]="Dimanche";
   localStorage["objectSelect"] = JSON.stringify(tab);
   
	
	
	
} else {
  alert("localStorage n'est pas supporté");
}
 
 
 
})
 