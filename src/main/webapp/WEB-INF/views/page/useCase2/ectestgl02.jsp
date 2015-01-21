<!DOCTYPE html> 
<html lang="en">
	<head>
		<meta name="viewport" content="width=device-width, initial-scale=1" />
		<meta charset="UTF-8" />
		<!-- Latest compiled and minified CSS --> 
		<link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css" />
		<!-- Optional theme --> 
		<link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap-theme.min.css" />
		<link rel="stylesheet" href="http://ivaynberg.github.io/select2/select2-3.5.1/select2.css" />
		<script src="http://code.jquery.com/jquery-1.11.0.min.js"></script> <!-- Latest compiled and minified JavaScript --> <script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script> <script src="http://ivaynberg.github.io/select2/select2-3.5.1/select2.js"></script> 
		<style> body { font-size:100%; font: 12x Helvetica; background: #999999; @media (max-width: 640px) { body {font-size:1rem;} } @media (min-width: 640px) { body {font-size:0.6rem;} } @media (min-width:960px) { body {font-size:0.8rem;} } @media (min-width:1100px) { body {font-size:0.8rem;} } } #main { margin: 0px; padding: 0px; width:100%; height:100%; } header, footer { display: block; margin: 4px; padding: 5px; min-height: 100px; border: 1px solid #eebb55; border-radius: 7pt; background: #ffeebb; } </style>
		**************** include directory=directory file=fichier **************** include directory=directory file=fichier <script>
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
			
		</script> <script>
			$(function () {
			$('#select20').empty();
			
			 
			var  tab = JSON.parse(localStorage["objectSelect"]);
			 
			tab.forEach(function(y) { 
			var option="<option>" + y + "</option>";
			$('#select20').append(option);
			 }); 
				
				$('#select20').select2();
			})
			  
			 
			  
			$(function () {
			$('#select32').empty();
			
			 
			var  tab = JSON.parse(localStorage["objectSelect"]);
			 
			tab.forEach(function(y) { 
			var option="<option>" + y + "</option>";
			$('#select32').append(option);
			 }); 
				
				$('#select32').select2();
			})
			  
			 
			  
			
		</script> 
	</head>
	<body>
		<header>header</header>
		<div id="main">
			<test>valeur du champ</test>
			<div class="container-fluid" style="background-color:#eee;border: 1px solid #888;border-radius:3px;">
				<div class="row">
					<div class="col-md-11 col-md-offset-0">
						<div class="container-fluid" style="background-color:#eee;border: 1px solid #888;border-radius:3px;">
							<div class="row">
								<div class="col-md-2 col-md-offset-0">
									<h1>A Big Title </h1>
								</div>
								<div class="col-md-2 col-md-offset-1"> <span class="help-block" width="15" height="3" id="label24" class="control-label">A <a href="">paragraph</a> of <b>text</b>.<br />A <i>second</i> <ins>row</ins> of <del>text</del>. </span></div>
								<div class="col-md-2 col-md-offset-2"> <span id="label25" class="label label-default">A comment </span></div>
							</div>
							<div class="row">
								<div class="col-md-1 col-md-offset-5"> </div>
							</div>
						</div>
					</div>
				</div>
				<div class="row">
					<div class="col-md-3 col-md-offset-0">
						<div class="container-fluid" style="background-color:#eee;border: 1px solid #888;border-radius:3px;">
							<div class="row">
								<div class="col-md-10 col-md-offset-1"> <textarea class="form-control" width="17" height="6">ceci est un exemple de texte _texte grise_ *texte surBrillant* </textarea></div>
							</div>
							<div class="row">
								<div class="col-md-10 col-md-offset-2"> <img id="img21" src="./assets/visa_logo.png" alt="" /> </img></div>
							</div>
						</div>
					</div>
					<div class="col-md-4 col-md-offset-0">
						<div class="container-fluid" style="background-color:#eee;border: 1px solid #888;border-radius:3px;">
							<div class="row">
								<div class="col-md-2 col-md-offset-2"> <label id="label3" class="control-label" for="personne_textinput__1">Adresse </label></div>
								<div class="col-md-5 col-md-offset-1"> <input type="text" class="form-control" id="textinput1" placeholder="text1" autocomplete="on" maxlength="10" name="adresseDto.rue" value="adresseDto.rue" /> </div>
							</div>
							<div class="row">
								<div class="col-md-2 col-md-offset-2"> <label id="label2" class="control-label" for="personne_checkboxgroup_notDefined_41">Personne </label></div>
								<div class="col-md-4 col-md-offset-1">
									<div class="checkbox" id="checkbox41Cpt1"> <input type="checkbox" value="" /> <label> not selected</label> </div>
									<div class="checkbox" id="checkbox41Cpt2"> <input type="checkbox" value="" checked="yes" /> <label> selected</label> </div>
								</div>
							</div>
							<div class="row">
								<div class="col-md-2 col-md-offset-2"> <label id="label5" class="control-label" for="personne_checkbox_notDefined_4">Homme </label></div>
								<div class="col-md-2 col-md-offset-1">
									<div class="checkbox" id="checkbox4"> <input type="checkbox" value="" disabled="true" /><label> Libelle </label> </div>
								</div>
								<div class="col-md-2 col-md-offset-1">
									<div class="radio" id="radio6"> <input type="radio" value="" /> <label> Marie </label> </div>
								</div>
							</div>
							<div class="row">
								<div class="col-md-4 col-md-offset-2"> <span class="help-block" width="9" height="2" id="label7" class="control-label">491 rue haute<br />59870 Bouvignies. </span></div>
							</div>
						</div>
					</div>
					<div class="col-md-4 col-md-offset-0">
						<div class="container-fluid" style="background-color:#eee;border: 1px solid #888;border-radius:3px;">
							<div class="row">
								<div class="col-md-2 col-md-offset-1">
									<div class="checkbox" id="checkbox16"> <input type="checkbox" value="" /><label> H/F </label> </div>
								</div>
							</div>
							<div class="row">
								<div class="col-md-7 col-md-offset-1">
									<select id="select20" class="select2" multiple="multiple">
										<option>ComboBox</option>
									</select>
								</div>
							</div>
							<div class="row">
								<div class="col-md-7 col-md-offset-1"> <input type="number" class="form-control" id="input17" placeholder="3" value="" name="input17" /> </div>
							</div>
							<div class="row">
								<div class="col-md-7 col-md-offset-1"> <input type="date" class="form-control" id="input18" placeholder="" name="input18" /></div>
							</div>
							<div class="row">
								<div class="col-md-4 col-md-offset-1"> <input type="search" class="form-control" id="input19" placeholder="search" value="" name="input19" /></div>
							</div>
						</div>
					</div>
				</div>
				<div class="row">
					<div class="col-md-11 col-md-offset-0">
						<div class="container-fluid" style="background-color:#eee;border: 1px solid #888;border-radius:3px;">
							<div class="row">
								<div class="col-md-3 col-md-offset-4">
									<h2>A Subtitle</h2>
								</div>
							</div>
							<div class="row">
								<div class="col-md-1 col-md-offset-3"> <input type="text" class="form-control" id="notDefined_textinput_notDefined_12" placeholder="" autocomplete="on" maxlength="10" /> </div>
								<div class="col-md-1 col-md-offset-0"> <input type="text" class="form-control" id="notDefined_textinput_notDefined_14" placeholder="" autocomplete="on" maxlength="10" /> </div>
								<div class="col-md-1 col-md-offset-0"> <input type="text" class="form-control" id="notDefined_textinput_notDefined_15" placeholder="" autocomplete="on" maxlength="10" /> </div>
							</div>
						</div>
					</div>
				</div>
				<div class="row">
					<div class="col-md-11 col-md-offset-0">
						<div class="container-fluid" style="background-color:#eee;border: 1px solid #888;border-radius:3px;">
							<div class="row">
								<div class="col-md-1 col-md-offset-5"> <label id="label30" class="control-label">Titre </label></div>
							</div>
							<div class="row">
								<div class="col-md-3 col-md-offset-0">
									<div class="list-group" id="div29"> <a href="#" class="list-group-item"> <span class="glyphicon glyphicon-camera">Un</span> <span class="badge">25</span> </a> <a href="#" class="list-group-item"> <span class="glyphicon glyphicon-camera">Deux</span> <span class="badge">25</span> </a> <a href="#" class="list-group-item"> <span class="glyphicon glyphicon-camera">Trois</span> <span class="badge">25</span> </a> <a href="#" class="list-group-item"> <span class="glyphicon glyphicon-camera">Quatre</span> <span class="badge">25</span> </a> </div>
								</div>
								<div class="col-md-3 col-md-offset-1">
									<ul class="nav nav-tabs  tabs-left" role="tablist" id="tabbar28">
										<li><a href="" role="tab" data-toggle="tab">One</a></li>
										<li><a href="" role="tab" data-toggle="tab">Two</a></li>
										<li><a href="testgl01" role="tab" data-toggle="tab">Three</a></li>
										<li><a href="#" role="tab" data-toggle="tab">Four</a></li>
									</ul>
									<div class="tabbable">
										<div class="tab-content">
											<div class="tab-pane" id="tab1"> </div>
										</div>
									</div>
								</div>
								<div class="col-md-2 col-md-offset-1">
									<select id="select32" class="select2" multiple="multiple">
										<option>ComboBox</option>
									</select>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="row">
					<div class="col-md-11 col-md-offset-0">
						<div class="container-fluid" style="background-color:#eee;border: 1px solid #888;border-radius:3px;">
							<div class="row">
								<div class="col-md-7 col-md-offset-0">
									<div id="div26" class="table-responsive">
										<table id="table26" class="table table-bordered">
											<thead>
												<tr>
													<th style="text-align:left"> Name<br />(job title) </th>
													<th style="text-align:right"> Age </th>
													<th> Nickname </th>
													<th style="text-align:center"> Employee </th>
												</tr>
											</thead>
											<tr>
												<td style="text-align:left"> <input type="submit" class="btn btn-default" id="label27" value="OK" /> </td>
												<td style="text-align:right">
													<div class="list-group" id="div36"> <a href="#" class="list-group-item"> <span class="glyphicon glyphicon-camera">Item One</span> <span class="badge">25</span> </a> <a href="#" class="list-group-item"> <span class="glyphicon glyphicon-camera">Item Two</span> <span class="badge">25</span> </a> <a href="#" class="list-group-item"> <span class="glyphicon glyphicon-camera">Item Three</span> <span class="badge">25</span> </a> </div>
												</td>
												<td> <input type="text" class="form-control" id="notDefined_textinput_notDefined_37" placeholder="" autocomplete="on" maxlength="10" name="input1" value="input1" /> </td>
												<td style="text-align:center"> <input type="text" class="form-control" id="notDefined_textinput_notDefined_38" placeholder="" autocomplete="on" maxlength="10" name="input2" value="input2" /> </td>
											</tr>
										</table>
									</div>
								</div>
								<div class="col-md-2 col-md-offset-0">
									<div class="checkbox" id="checkbox22Cpt1"> <input type="checkbox" value="" /> <label> not selected</label> </div>
									<div class="checkbox" id="checkbox22Cpt2"> <input type="checkbox" value="" checked="yes" /> <label> selected</label> </div>
									<div class="checkbox" id="checkbox22Cpt3"> <input type="checkbox" value="" /> <label> indeterminate</label> </div>
									<div class="checkbox" id="checkbox22Cpt4"> <input type="checkbox" value="" disabled="true" /> <label> disabled</label> </div>
									<div class="checkbox" id="checkbox22Cpt5"> <input type="checkbox" value="" disabled="true" checked="yes" /> <label> disabled selected</label> </div>
									<div class="checkbox" id="checkbox22Cpt6"> <input type="checkbox" value="" disabled="true" /> <label> disabled indeterminate</label> </div>
									<div class="checkbox" id="checkbox22Cpt7"> <input type="checkbox" value="" /> <label> ????</label> </div>
								</div>
								<div class="col-md-2 col-md-offset-1">
									<div class="radio" id="radio23Cpt1"> <input type="radio" value="" checked="yes" /> <label>option 1 (selected)</label> </div>
									<div class="radio" id="radio23Cpt2"> <input type="radio" value="" /> <label>option 2</label> </div>
									<div class="radio" id="radio23Cpt3"> <input type="radio" value="" /> <label>option 3 (indeterminate)</label> </div>
									<div class="radio" id="radio23Cpt4"> <input type="radio" value="" disabled="true" /> <label>option 4 (disabled)</label> </div>
									<div class="radio" id="radio23Cpt5"> <input type="radio" value="" disabled="true" checked="yes" /> <label>option 5<br />(disabled and selected)</label> </div>
									<div class="radio" id="radio23Cpt6"> <input type="radio" value="" disabled="true" /> <label>option 6<br />(disabled indeterminate)</label> </div>
									<div class="radio" id="radio23Cpt7"> <input type="radio" value="" /> <label>????</label> </div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
		<footer></footer>
	</body>
</html>
